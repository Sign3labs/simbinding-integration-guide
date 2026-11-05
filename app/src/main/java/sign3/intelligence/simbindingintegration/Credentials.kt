package sign3.intelligence.simbindingintegration

/**
 * Credentials are injected at build time from the git-ignored credentials.properties
 * at the project root (see app/build.gradle.kts). Never hardcode secrets here.
 */
object Credentials {
    const val TENANT_ID = BuildConfig.SIGN3_TENANT_ID
    const val TENANT_SECRET = BuildConfig.SIGN3_TENANT_SECRET
}
