# Sign3 SIM Binding SDK Integration Guide for Android

The Sign3 SIM Binding SDK verifies that the phone number a user claims is the number of the SIM in the device they are holding. It does this carrier-side, over the cellular data interface, through Silent Network Authentication (SNA): nothing is sent to the user. When the carrier cannot answer, the flow falls back to an SMS OTP, which the SDK reads and verifies on its own.

The SDK is headless and is driven entirely by the Sign3 Intelligence SDK. There is nothing to call and nothing to handle on the device; you only read the transaction id off the intelligence response and, from your backend, ask the Sign3 status API what became of it.

<br>

## Adding the SIM Binding SDK to Your Project

1. **Configure the Repository in `settings.gradle`**
   - Open your project's `settings.gradle` file and add the Sign3 JFrog repository to the `dependencyResolutionManagement` block. Please collect the **username** and **password** from the credentials document.

     ```groovy
     dependencyResolutionManagement {
         repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
         repositories {
             google()
             mavenCentral()
             maven { url 'https://jitpack.io' }

             // JFrog repository to pull Sign3 SDK artifacts
             maven {
                 url "https://sign3.jfrog.io/artifactory/intelligence-generic-local/"
                 credentials {
                     username = "provided in credential doc"
                     password = "provided in credential doc"
                 }
             }
         }
     }
     ```

2. **Add the SIM Binding SDK Dependency in App-Level Gradle**

   ```groovy
   dependencies {
       // Sign3 Intelligence, which drives SIM binding
       implementation 'com.sign3.intelligence:intelligence-playstore-lite:5.x.x'

       // Sign3 SIM Binding
       implementation 'com.sign3.simbinding:intelligence-playstore:1.0.0'
   }
   ```
   - Checkout the [latest version](#changelog)
   - The SDK brings its own dependencies. Nothing else is needed for SIM binding or SMS OTP.
   - The SDK requires a minimum SDK version of 23. If your app targets below this version, enclose the calls within conditional checks.

3. **After adding the dependency, sync your project with Gradle files to ensure the library is properly integrated.**

<br>

## App Permission

Add the following permissions to your app's `AndroidManifest.xml`. The SDK itself declares only `INTERNET`; the rest belong to your app because SNA has to reach the carrier over the cellular data interface and has to know which SIM is active.

```permission
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

### Network security config

Once you have successfully integrated the Sign3 SIM Binding SDK in your application, add the following line in your app's AndroidManifest file in the `<application>` tag:

```xml
<application
    android:networkSecurityConfig="@xml/sign3_network_security_config"
    ... >
```

<br>

## Initializing the SDK

SIM binding has no initialisation of its own. Initialise the Sign3 Intelligence SDK in the `onCreate()` of your Application class, exactly as in the [Sign3 SDK integration guide](https://github.com/Sign3labs/sdk-integration-guide-lite), using the ClientID and Client Secret from the credentials document.

### For Kotlin

```kotlin
override fun onCreate() {
    super.onCreate()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val options = Options.Builder()
            .setClientId("<SIGN3_CLIENT_ID>")
            .setClientSecret("<SIGN3_CLIENT_SECRET>")
            .setSSLPinning(true) // Optional, default false
            .setEnvironment(if (BuildConfig.DEBUG) Options.ENV_DEV else Options.ENV_PROD)
            .build()

        Sign3Intelligence.getInstance(this).initAsync(options) {
            Log.i("TAG_AppInstance", "Sign3Intelligence init : $it")
        }
    }
}
```

### For Java

```java
@Override
public void onCreate() {
    super.onCreate();

    Options options = new Options.Builder()
            .setClientId("<SIGN3_CLIENT_ID>")
            .setClientSecret("<SIGN3_CLIENT_SECRET>")
            .setSSLPinning(true) // Optional, default false
            .setEnvironment(BuildConfig.DEBUG ? Options.ENV_DEV : Options.ENV_PROD)
            .build();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Sign3Intelligence.getInstance(this).initAsync(options);
    }
}
```
<br>

## Starting SIM Binding

You do not call the SIM binding SDK yourself. The Sign3 Intelligence SDK runs SIM binding as part of a login or signup score:

1. Ask Sign3 to enable SIM binding for your tenant.
2. Set the user's phone number through `updateOptions`, **with the country code and no `+` or spaces** (`919876543210`), and a `LOGIN` or `SIGNUP` event type. SIM binding does not run for `TRANSACTION` or `OTHERS`.
3. Call `getIntelligence()`.

On that score the Intelligence SDK brings the SIM binding engine up, binds the SIM under the transaction the backend opened, and when the carrier cannot answer and an OTP is sent instead, reads the OTP and verifies it. The score response carries the transaction id as `IntelligenceResponse.snaRequestID` — hold on to it, it is what the status API is asked about.

Options are reset after every score, so update them again before each login or signup.

### For Kotlin

```kotlin
val updateOptions = UpdateOptions.Builder()
    .setPhoneNumber("919876543210")        // country code + number, digits only
    .setUserEventType(UserEventType.LOGIN) // LOGIN or SIGNUP
    .build()

Sign3Intelligence.getInstance(this).updateOptions(updateOptions)

Sign3Intelligence.getInstance(this).getIntelligence(object : IntelligenceListener {
    override fun onSuccess(response: IntelligenceResponse) {
        val snaRequestId = response.snaRequestID
        if (snaRequestId.isNullOrEmpty()) {
            // SIM binding did not start for this score
        } else {
            // SIM binding is running. Send this id to your backend for the status check.
            Log.i("Sign3SimBinding", "SIM binding under $snaRequestId")
        }
    }

    override fun onError(error: IntelligenceError) {
        // Something went wrong, handle the error message
    }
})
```

### For Java

```java
UpdateOptions updateOptions = new UpdateOptions.Builder()
        .setPhoneNumber("919876543210")        // country code + number, digits only
        .setUserEventType(UserEventType.LOGIN) // LOGIN or SIGNUP
        .build();

Sign3Intelligence.getInstance(this).updateOptions(updateOptions);

Sign3Intelligence.getInstance(this).getIntelligence(new IntelligenceListener() {
    @Override public void onSuccess(IntelligenceResponse response) {
        String snaRequestId = response.getSnaRequestID();
        Log.i("Sign3SimBinding", "SIM binding under " + snaRequestId);
    }

    @Override public void onError(IntelligenceError error) { }
});
```
<br>

## Checking the SIM Binding Status

The score returns as soon as the transaction is open; the binding itself finishes afterwards. Ask the status API what became of the `snaRequestID`.

**Call this from your backend.** The credentials below are your tenant id and tenant secret, and they must not ship in the app. The sample app calls it from the device only so the flow can be demonstrated on one screen.

### Request

```bash
curl --location 'https://intelligence-staging.sign3.in/auth/v1/status?requestId=ARID_411A767110E0422FA06F9CF14F2B8E34' \
--header 'Content-Type: application/json' \
--header 'Authorization: Basic <base64(tenantId:tenantSecret)>'
```

| | |
|---|---|
| Method | `GET` |
| Path | `/auth/v1/status` |
| Query | `requestId` — the `snaRequestID` from `IntelligenceResponse` |
| `Authorization` | `Basic` over `<tenantId>:<tenantSecret>`, both provided by Sign3 |

`auths[].status` is what you act on; it is `PENDING` until the carrier answers, so poll until it settles or you time out.

### `200` — the transaction is known

```json
{
  "auths": [
    {
      "identityType": "MOBILE",
      "identityValue": "917069914791",
      "channel": "SILENT_AUTH",
      "methods": ["SILENT_AUTH"],
      "status": "PENDING",
      "type": "PRIMARY"
    }
  ],
  "derivedOperator": "AIRTEL",
  "phoneDetail": {
    "countryCode": "91",
    "country": "IN",
    "type": "MOBILE",
    "homeOperator": "VI",
    "location": "India",
    "timeZones": ["Asia/Calcutta"]
  },
  "simDetail": {
    "operator": "AIRTEL",
    "mcc": 405,
    "mnc": 51
  },
  "networkDetail": {
    "ip": "2401:4900:1c50:1a3b::1",
    "ipType": "IPV6",
    "operator": "AIRTEL"
  },
  "deviceFingerprinting": {
    "status": "SUCCESS",
    "sessionId": "a98ead44-f4db-4801-8c3f-041f98140734",
    "deviceId": "781b21f7-220b-4f44-9155-6261f8564924",
    "newDevice": false,
    "riskAssessment": {
      "sessionRiskLevel": "HIGH",
      "deviceRiskLevel": "HIGH",
      "sessionRiskScore": 95,
      "deviceRiskScore": 90,
      "ipFraudScore": 0,
      "flags": {
        "isVpn": false,
        "isEmulator": false,
        "isAppTampered": true
      }
    },
    "deviceContext": {
      "brand": "iQOO",
      "model": "I2410",
      "os": "Android",
      "osVersion": "16"
    },
    "networkContext": {
      "ipAddress": "106.205.222.198",
      "ipType": "v4",
      "asn": "45609",
      "isp": "Bharti Airtel Limited"
    }
  }
}
```

A mismatch between `derivedOperator` (the SIM that actually answered) and `phoneDetail.homeOperator` is normal on a ported number.

### `400` — asked too early

```json
{
  "message": "Invalid Request",
  "errorCode": "7170",
  "description": "Auth not started yet. Please initiate authentication first."
}
```

The transaction has not opened yet. Retry after a moment; if it never opens, SIM binding did not start for that score.

### `401` — credentials rejected

```json
{
  "message": "Access blocked",
  "errorCode": "7012",
  "description": "Authorization error: Merchant credentials are empty"
}
```

The `Authorization` header is missing, malformed, or carries the wrong tenant id / secret.

<br>

## Changelog
### 1.0.0
 - Silent Network Authentication over the carrier network, with automatic fallback to SMS OTP and automatic OTP verification.
 - Driven entirely by the Sign3 Intelligence SDK on login and signup scores; no API to call.
 - `IntelligenceResponse.snaRequestID` carries the transaction id for the `/auth/v1/status` check.
 - Ships its network security config and consumer ProGuard rules.
