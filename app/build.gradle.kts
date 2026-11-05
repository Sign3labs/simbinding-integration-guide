import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Private credentials live in credentials.properties at the project root (git-ignored).
val credentialsProperties = Properties().apply {
    val file = rootProject.file("credentials.properties")
    require(file.exists()) {
        "Missing ${file.absolutePath}. Copy credentials.properties.example and fill in the values."
    }
    file.inputStream().use { load(it) }
}

fun credential(key: String): String =
    credentialsProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: error("Missing '$key' in credentials.properties")

android {
    namespace = "sign3.intelligence.simbindingintegration"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "sign3.intelligence.simbindingintegration"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "SIGN3_TENANT_ID", "\"${credential("sign3.tenantId")}\"")
        buildConfigField("String", "SIGN3_TENANT_SECRET", "\"${credential("sign3.tenantSecret")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Sign3 Intelligence, which drives SIM binding
    implementation("com.sign3.intelligence:intelligence-playstore-lite:0.0.1000")
    // Sign3 SIM Binding
    implementation("com.sign3.simbinding:intelligence-playstore:1.0.0")
}