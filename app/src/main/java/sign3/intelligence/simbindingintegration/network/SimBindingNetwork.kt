package sign3.intelligence.simbindingintegration.network

import android.content.Context
import android.util.Base64
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sign3.intelligence.simbindingintegration.Credentials
import java.util.concurrent.TimeUnit

object SimBindingNetwork {
    private const val BASE_URL = "https://intelligence.sign3.in/"

    fun createApi(context: Context): SimBindingApi {
        val credentials = Base64.encodeToString(
            "${Credentials.TENANT_ID}:${Credentials.TENANT_SECRET}".toByteArray(),
            Base64.NO_WRAP
        )
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Basic $credentials")
                        .build()
                )
            }
            // Real interceptor in debug builds, no-op in release
            .addInterceptor(ChuckerInterceptor.Builder(context.applicationContext).build())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SimBindingApi::class.java)
    }
}
