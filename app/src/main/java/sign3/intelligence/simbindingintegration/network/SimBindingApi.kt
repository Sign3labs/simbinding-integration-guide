package sign3.intelligence.simbindingintegration.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SimBindingApi {
    @GET("auth/v1/status")
    suspend fun getStatus(@Query("requestId") requestId: String): Response<StatusResponse>
}
