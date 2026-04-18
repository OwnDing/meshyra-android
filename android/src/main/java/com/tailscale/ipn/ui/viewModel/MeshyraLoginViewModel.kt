// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.viewModel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.tailscale.ipn.App
import com.tailscale.ipn.R
import com.tailscale.ipn.ui.model.Ipn
import com.tailscale.ipn.ui.network.CaptchaResponseData
import com.tailscale.ipn.ui.network.DeviceLoginRequest
import com.tailscale.ipn.ui.network.MeshyraApiException
import com.tailscale.ipn.ui.network.MeshyraAuthApi
import com.tailscale.ipn.ui.notifier.Notifier
import com.tailscale.ipn.ui.util.set
import com.tailscale.ipn.util.TSLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeshyraLoginViewModel : IpnViewModel() {

  val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
  val loadingMessageRes: StateFlow<Int?> = MutableStateFlow(null)
  val isCaptchaLoading: StateFlow<Boolean> = MutableStateFlow(false)
  val captcha: StateFlow<CaptchaResponseData?> = MutableStateFlow(null)
  val errorMessage: StateFlow<String?> = MutableStateFlow(null)
  private val waitingForBackendLogin: StateFlow<Boolean> = MutableStateFlow(false)
  private var pendingOnSuccess: (() -> Unit)? = null

  init {
    viewModelScope.launch {
      Notifier.state.collect { state ->
        if (waitingForBackendLogin.value && state != Ipn.State.NeedsLogin) {
          stopLoading()
          waitingForBackendLogin.set(false)
          pendingOnSuccess?.invoke()
          pendingOnSuccess = null
        }
      }
    }
    refreshCaptcha()
  }

  fun refreshCaptcha() {
    viewModelScope.launch {
      isCaptchaLoading.set(true)
      val result = MeshyraAuthApi.fetchCaptcha()
      isCaptchaLoading.set(false)
      result
          .onSuccess { captcha.set(it) }
          .onFailure {
            TSLog.e("MeshyraLogin", "Failed to fetch captcha: $it")
            errorMessage.set(App.get().applicationContext.getString(R.string.network_error))
          }
    }
  }

  fun performLogin(username: String, password: String, captchaCode: String, onSuccess: () -> Unit) {
    val context: Context = App.get().applicationContext
    if (username.isBlank() || password.isBlank() || captchaCode.isBlank()) {
      errorMessage.set(context.getString(R.string.meshyra_login_error_missing_fields))
      return
    }
    val captchaId = captcha.value?.captchaId
    if (captchaId.isNullOrBlank()) {
      errorMessage.set(context.getString(R.string.meshyra_login_error_captcha_not_ready))
      refreshCaptcha()
      return
    }

    viewModelScope.launch {
      errorMessage.set(null)
      isLoading.set(true)
      waitingForBackendLogin.set(false)
      pendingOnSuccess = null
      loadingMessageRes.set(R.string.meshyra_login_loading_authenticating)
      val loginResult =
          MeshyraAuthApi.deviceLogin(
              DeviceLoginRequest(
                  username = username,
                  password = password,
                  captchaCode = captchaCode,
                  captchaId = captchaId,
              ))

      loginResult
          .onFailure {
            TSLog.e("MeshyraLogin", "Backend login failed: $it")
            val message =
                when (it) {
                  is MeshyraApiException -> it.message
                  else -> null
                }
            errorMessage.set(message ?: context.getString(R.string.network_error))
            waitingForBackendLogin.set(false)
            pendingOnSuccess = null
            stopLoading()
            refreshCaptcha()
          }
          .onSuccess { data ->
            val prefs = Ipn.MaskedPrefs()
            prefs.ControlURL = data.serverUrl
            loadingMessageRes.set(R.string.meshyra_login_loading_connecting)
            pendingOnSuccess = onSuccess

            login(prefs, authKey = data.authKey) { result ->
              result
                  .onSuccess {
                    errorMessage.set(null)
                    waitingForBackendLogin.set(true)
                    loadingMessageRes.set(R.string.meshyra_login_loading_finishing)
                    if (Notifier.state.value != Ipn.State.NeedsLogin) {
                      stopLoading()
                      waitingForBackendLogin.set(false)
                      pendingOnSuccess?.invoke()
                      pendingOnSuccess = null
                    }
                  }
                  .onFailure { t ->
                    waitingForBackendLogin.set(false)
                    pendingOnSuccess = null
                    stopLoading()
                    TSLog.e("MeshyraLogin", "Headscale login failed: $t")
                    errorMessage.set(t.message ?: context.getString(R.string.login_failed))
                  }
            }
          }
    }
  }

  private fun stopLoading() {
    isLoading.set(false)
    loadingMessageRes.set(null)
  }
}
