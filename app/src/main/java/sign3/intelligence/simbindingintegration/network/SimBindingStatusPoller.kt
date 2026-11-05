package sign3.intelligence.simbindingintegration.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.delay
import retrofit2.Response

sealed interface SimBindingResult {
    data class Success(val phoneNumber: String?, val operator: String?) : SimBindingResult
    data class Failed(val errorCode: String?, val message: String?) : SimBindingResult
    data class Error(val errorCode: String?, val message: String?) : SimBindingResult
    data object Timeout : SimBindingResult
}

/** Polls GET /auth/v1/status until the SIM binding is SUCCESS or FAILED, or attempts run out. */
object SimBindingStatusPoller {
    private const val TAG = "SimBindingStatusPoller"

    private const val POLL_INTERVAL_MS = 2_000L
    private const val MAX_ATTEMPTS = 60

    // 400 / 7170 "Auth not started yet" is transient right after onSuccess, so keep polling
    private const val ERROR_AUTH_NOT_STARTED = "7170"

    suspend fun poll(api: SimBindingApi, snaRequestId: String): SimBindingResult {
        repeat(MAX_ATTEMPTS) { attempt ->
            val response = try {
                api.getStatus(snaRequestId)
            } catch (e: Exception) {
                Log.e(TAG, "Status poll #${attempt + 1} failed", e)
                null // network problem, retry
            }

            if (response != null) {
                // errorBody() is single-read, so capture it once and share it with parse()
                val errorText = if (response.isSuccessful) null else response.errorBody()?.string()
                Log.i(
                    TAG,
                    "Status poll #${attempt + 1}: ${response.code()} " +
                        (if (response.isSuccessful) Gson().toJson(response.body()) else errorText.orEmpty())
                )
                parse(response, errorText)?.let { return it }
            }
            delay(POLL_INTERVAL_MS)
        }
        Log.w(TAG, "Status polling timed out for $snaRequestId")
        return SimBindingResult.Timeout
    }

    /** Returns a final result, or null when we should keep polling. */
    private fun parse(response: Response<StatusResponse>, errorText: String?): SimBindingResult? {
        // 5xx (e.g. 502 "status lookup failed") is a transient server problem, so retry
        if (response.code() >= 500) return null

        if (!response.isSuccessful) {
            val error = runCatching {
                Gson().fromJson(errorText, ApiErrorResponse::class.java)
            }.getOrNull()
            if (error?.errorCode == ERROR_AUTH_NOT_STARTED) return null
            return SimBindingResult.Error(
                error?.errorCode,
                error?.description?.ifEmpty { null } ?: error?.message ?: "HTTP ${response.code()}"
            )
        }

        val body = response.body() ?: return null
        val auth = body.auths?.firstOrNull() ?: return null
        return when (auth.status?.uppercase()) {
            "SUCCESS" -> SimBindingResult.Success(auth.identityValue, body.derivedOperator)
            "FAILED" -> SimBindingResult.Failed(
                auth.error?.errorCode,
                auth.error?.description ?: auth.error?.message
            )
            else -> null // PENDING
        }
    }
}
