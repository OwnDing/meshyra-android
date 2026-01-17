package com.tailscale.ipn.ui.viewModel

import androidx.lifecycle.viewModelScope
import com.tailscale.ipn.ui.model.Ipn
import com.tailscale.ipn.ui.util.set
import com.tailscale.ipn.ui.view.ErrorDialogType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MeshyraLoginViewModel : IpnViewModel() {
    
    val isLoading: StateFlow<Boolean> = MutableStateFlow(false)
    val errorDialog: StateFlow<ErrorDialogType?> = MutableStateFlow(null)
    
    // Mock response data
    data class LoginResponse(val authKey: String, val controlUrl: String)

    fun performLogin(username: String, password: String, captcha: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank() || captcha.isBlank()) {
            // using generic error for now, ideally we should have a specific one or toast
            errorDialog.set(ErrorDialogType.LOGIN_FAILED) 
            return
        }

        viewModelScope.launch {
            isLoading.set(true)
            
            // SIMULATE NETWORK REQUEST
            delay(1500) 
            
            // Hardcoded mock success for "test" user (or any user for now to make it easy to test)
            // In a real app, validate credentials here.
            val mockResponse = LoginResponse(
                authKey = "tskey-auth-kCY222CNTRL-mockAuthKeyForMeshyraClient123", // Example format
                controlUrl = "https://controlplane.tailscale.com" // Standard or custom URL
            )

            // Proceed to login with Tailscale backend
            loginWithMeshyraCredentials(mockResponse.authKey, mockResponse.controlUrl) { result ->
                isLoading.set(false)
                result.onSuccess {
                    onSuccess()
                }.onFailure {
                    errorDialog.set(ErrorDialogType.ADD_PROFILE_FAILED)
                }
            }
        }
    }

    private fun loginWithMeshyraCredentials(authKey: String, controlUrl: String, completionHandler: (Result<Unit>) -> Unit) {
        val prefs = Ipn.MaskedPrefs()
        prefs.ControlURL = controlUrl
        prefs.WantRunning = true
        prefs.LoggedOut = false
        
        login(prefs, authKey = authKey, completionHandler)
    }
}
