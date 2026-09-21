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

1. Initialize the SDK in the `onCreate()` method of your Application class.
2. Use the ClientID and Client Secret shared with the credentials document.
3. The SDK require a minimum SDK version of 23 if your app is targeting below this version must enclose Sign3 API calls within conditional checks.

### For Kotlin

```kotlin
override fun onCreate() {
   super.onCreate()

   // Other initialisation code
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val options = Options.Builder()
         .setClientId("<SIGN3_CLIENT_ID>")
         .setClientSecret("<SIGN3_CLIENT_SECRET>")
         .setSSLPinning(true) // Optional: If you want SSL pinning in API calls, default value is false.
         .setEnvironment(if (BuildConfig.DEBUG) Options.ENV_DEV else Options.ENV_PROD) // For Prod: Options.ENV_PROD, For Dev: Options.ENV_DEV
         .build()

      Sign3Intelligence.getInstance(this).initAsync(options) {
         // to check if the SDK is initialized correctly or not
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

   // Other initialisation code
   Options options = new Options.Builder()
           .setClientId("<SIGN3_CLIENT_ID>")
           .setClientSecret("<SIGN3_CLIENT_SECRET>")
           .setSSLPinning(true) // Optional: If you want SSL pinning in API calls, default value is false.
           .setEnvironment(BuildConfig.DEBUG ? Options.ENV_DEV : Options.ENV_PROD) // For Prod: Options.ENV_PROD, For Dev: Options.ENV_DEV
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
   @Override
   public void onSuccess(IntelligenceResponse response) {
      String snaRequestId = response.getSnaRequestID();
      if (snaRequestId == null || snaRequestId.isEmpty()) {
         // SIM binding did not start for this score
      } else {
         // SIM binding is running. Send this id to your backend for the status check.
         Log.i("Sign3SimBinding", "SIM binding under " + snaRequestId);
      }
   }

   @Override
   public void onError(IntelligenceError error) {
      // Something went wrong, handle the error message
   }
});
```
<br>

## Checking the SIM Binding Status

The score returns as soon as the transaction is open; the binding itself finishes afterwards. Ask the status API what became of the `snaRequestID`.

**Call this from your backend.** The credentials below are your tenant id and tenant secret, and they must not ship in the app. The sample app calls it from the device only so the flow can be demonstrated on one screen.

### Request

```bash
curl --location 'https://intelligence.sign3.in/auth/v1/status?requestId=ARID_411A767110E0422FA06F9CF14F2B8E34' \
--header 'Content-Type: application/json' \
--header 'Authorization: Basic <base64(tenantId:tenantSecret)>'
```

| | |
|---|---|
| Base URL | `https://intelligence.sign3.in` |
| Method | `GET` |
| Path | `/auth/v1/status` |
| Query | `requestId` — the `snaRequestID` from `IntelligenceResponse` |
| `Authorization` | `Basic` over `<tenantId>:<tenantSecret>`, both provided by Sign3 |

### Response

<details open>
<summary><b>&nbsp;<code>200</code> &nbsp;·&nbsp; The transaction is known</b></summary>

<details>
<summary>🟡 &nbsp;<b><code>PENDING</code></b> &nbsp;— the carrier has not answered yet</summary>

```json
{
  "auths": [
    {
      "identityType": "MOBILE",
      "identityValue": "917069914791",
      "channel": "SILENT_AUTH",
      "methods": [
        "SILENT_AUTH"
      ],
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
    "timeZones": [
      "Asia/Calcutta"
    ]
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
  }
}
```

</details>

<details>
<summary>✅ &nbsp;<b><code>SUCCESS</code></b> &nbsp;— the SIM is bound</summary>

```json
{
  "auths": [
    {
      "identityType": "MOBILE",
      "identityValue": "917069914791",
      "channel": "SILENT_AUTH",
      "methods": [
        "SILENT_AUTH"
      ],
      "status": "SUCCESS",
      "verifiedTimestamp": 1781091069000,
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
    "timeZones": [
      "Asia/Calcutta"
    ]
  },
  "simDetail": {
    "operator": "AIRTEL",
    "mcc": 405,
    "mnc": 51
  },
  "networkDetail": {
    "ip": "2401:4900:1c50:1a3b::1",
    "ipType": "IPV6",
    "operator": "AIRTEL",
    "callback": {
      "ip": "2401:4900:aabb:f09e::68fa:fe37",
      "operator": "AIRTEL",
      "userAgent": "Chrome/147.0.0.0 Mobile Safari/537.36"
    }
  }
}
```

</details>

<details>
<summary>❌ &nbsp;<b><code>FAILED</code></b> &nbsp;— the SIM could not be bound</summary>

```json
{
  "auths": [
    {
      "identityType": "MOBILE",
      "identityValue": "917069914791",
      "channel": "SILENT_AUTH",
      "methods": [
        "SILENT_AUTH"
      ],
      "status": "FAILED",
      "type": "PRIMARY",
      "error": {
        "errorCode": "SP40005",
        "message": "Operator not supported",
        "description": "This operator is not supported for verification. Please try with a different network."
      }
    }
  ],
  "derivedOperator": "JIO",
  "phoneDetail": {
    "countryCode": "91",
    "country": "IN",
    "type": "MOBILE",
    "homeOperator": "VI",
    "location": "India",
    "timeZones": [
      "Asia/Calcutta"
    ]
  },
  "simDetail": {
    "operator": "JIO",
    "mcc": 405,
    "mnc": 872
  },
  "networkDetail": {
    "ip": "49.204.148.177",
    "ipType": "IPV4"
  }
}
```

</details>

</details>

<details>
<summary><b>&nbsp;<code>400</code> &nbsp;·&nbsp; The request was not accepted</b></summary>

<details>
<summary>⚠️ &nbsp;<b><code>7170</code></b> &nbsp;— Auth not started yet</summary>

```json
{
  "message": "Invalid Request",
  "errorCode": "7170",
  "description": "Auth not started yet. Please initiate authentication first."
}
```

</details>

<details>
<summary>⚠️ &nbsp;<b><code>7119</code></b> &nbsp;— Invalid request Id</summary>

```json
{
  "message": "Invalid Request",
  "errorCode": "7119",
  "description": "Request error: Invalid request Id"
}
```

</details>

</details>

<details>
<summary><b>&nbsp;<code>401</code> &nbsp;·&nbsp; The caller was not authorised</b></summary>

<details>
<summary>🔒 &nbsp;<b><code>7012</code></b> &nbsp;— Merchant credentials are empty</summary>

```json
{
  "message": "Access blocked",
  "errorCode": "7012",
  "description": "Authorization error: Merchant credentials are empty"
}
```

</details>

<details>
<summary>🔒 &nbsp;<b><code>7002</code></b> &nbsp;— Invalid credentials</summary>

```json
{
  "message": "Access blocked",
  "errorCode": "7002",
  "description": "Authorization error: Invalid credentials"
}
```

</details>

<details>
<summary>🔒 &nbsp;<b><code>7019</code></b> &nbsp;— Merchant blocked</summary>

```json
{
  "message": "Merchant Blocked",
  "errorCode": "7019",
  "description": "Your account has been temporarily Blocked. Please contact support for assistance."
}
```

</details>

</details>

<br>

## Changelog
### 1.0.0
- Silent Network Authentication over the carrier network, with automatic fallback to SMS OTP and automatic OTP verification.
- Driven entirely by the Sign3 Intelligence SDK on login and signup scores; no API to call.
- `IntelligenceResponse.snaRequestID` carries the transaction id for the `/auth/v1/status` check.
- Ships its network security config and consumer ProGuard rules.
