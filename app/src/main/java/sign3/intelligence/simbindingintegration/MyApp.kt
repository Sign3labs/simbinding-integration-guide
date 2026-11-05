package sign3.intelligence.simbindingintegration

import android.app.Application
import android.util.Log
import com.sign3.public_classes.Sign3Intelligence
import com.sign3.public_classes.common.Options

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val options = Options.Builder()
            .setClientId(Credentials.TENANT_ID)
            .setClientSecret(Credentials.TENANT_SECRET)
            .setSSLPinning(true) // Optional: If you want SSL pinning in API calls, default value is false.
            .setEnvironment(if (BuildConfig.DEBUG) Options.ENV_DEV else Options.ENV_PROD) // For Prod: Options.ENV_PROD, For Dev: Options.ENV_DEV
            .build()

        Sign3Intelligence.getInstance(this).initAsync(options) {
            // to check if the SDK is initialized correctly or not
            Log.i(TAG, "Sign3Intelligence init : $it")
        }
    }

    companion object {
        private const val TAG = "MyApp"
    }
}