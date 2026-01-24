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
import com.tailscale.ipn.ui.util.set
import com.tailscale.ipn.util.TSLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeshyraLoginViewModel : IpnViewModel() {

  val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
  val isCaptchaLoading: StateFlow<Boolean> = MutableStateFlow(false)
  val captcha: StateFlow<CaptchaResponseData?> = MutableStateFlow(null)
  val errorMessage: StateFlow<String?> = MutableStateFlow(null)

  init {
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
      isLoading.set(true)
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
            isLoading.set(false)
            refreshCaptcha()
          }
          .onSuccess { data ->
            val prefs = Ipn.MaskedPrefs()
            prefs.ControlURL = data.serverUrl

            login(prefs, authKey = data.authKey) { result ->
              isLoading.set(false)
              result
                  .onSuccess {
                    errorMessage.set(null)
                    onSuccess()
                  }
                  .onFailure { t ->
                    TSLog.e("MeshyraLogin", "Headscale login failed: $t")
                    errorMessage.set(t.message ?: context.getString(R.string.login_failed))
                  }
            }
          }
    }
  }
}
