package sign3.intelligence.simbindingintegration.ui.screens

const val PHONE_LENGTH = 10
const val COUNTRY_CODE = "91"

data class WelcomeUiState(
    val phone: String = "",
    val submitted: Boolean = false,
    val isLoading: Boolean = false,
    val statusMessage: String? = null
) {
    val isValid: Boolean get() = phone.length == PHONE_LENGTH
    val showError: Boolean get() = submitted && !isValid
}
