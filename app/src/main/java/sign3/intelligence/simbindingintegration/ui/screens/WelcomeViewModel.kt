package sign3.intelligence.simbindingintegration.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sign3.public_classes.Sign3Intelligence
import com.sign3.public_classes.common.IntelligenceError
import com.sign3.public_classes.common.IntelligenceListener
import com.sign3.public_classes.common.IntelligenceResponse
import com.sign3.public_classes.common.UpdateOptions
import com.sign3.public_classes.common.UserEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sign3.intelligence.simbindingintegration.network.SimBindingResult
import sign3.intelligence.simbindingintegration.network.SimBindingStatusPoller
import sign3.intelligence.simbindingintegration.network.SimBindingNetwork

private const val TAG = "WelcomeViewModel"

class WelcomeViewModel(application: Application) : AndroidViewModel(application) {

    private val api = SimBindingNetwork.createApi(application)

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value.filter(Char::isDigit).take(PHONE_LENGTH)) }
    }

    fun onAuthenticateClick() {
        _uiState.update { it.copy(submitted = true) }
        val state = _uiState.value
        if (!state.isValid || state.isLoading) return

        _uiState.update { it.copy(isLoading = true, statusMessage = null) }
        authenticate(COUNTRY_CODE + state.phone)
    }

    private fun authenticate(phoneNumber: String) {
        val context = getApplication<Application>()
        val updateOptions = UpdateOptions.Builder()
            .setPhoneNumber(phoneNumber)           // country code + number, digits only
            .setUserEventType(UserEventType.AUTH).build()

        Sign3Intelligence.getInstance(context).updateOptions(updateOptions)

        Sign3Intelligence.getInstance(context).getIntelligence(object : IntelligenceListener {
            override fun onSuccess(response: IntelligenceResponse) {
                Log.d(TAG, "onSuccess: $response")
                pollStatus(response.snaRequestId!!)
            }

            override fun onError(error: IntelligenceError) {
                Log.i(TAG, "onError: ${error.errorMessage}")
                finish("Error: ${error.errorMessage}")
            }
        })
    }

    private fun pollStatus(snaRequestId: String) {
        viewModelScope.launch {
            val result = SimBindingStatusPoller.poll(api, snaRequestId)
            Log.i(TAG, "Final SIM binding status: $result")
            finish(
                when (result) {
                    is SimBindingResult.Success -> "SIM binding successful"
                    is SimBindingResult.Failed -> "SIM binding failed: ${result.message ?: result.errorCode}"
                    is SimBindingResult.Error -> "Status check error: ${result.message ?: result.errorCode}"
                    SimBindingResult.Timeout -> "Status check timed out"
                }
            )
        }
    }

    private fun finish(message: String) {
        _uiState.update { it.copy(isLoading = false, statusMessage = message) }
    }
}
