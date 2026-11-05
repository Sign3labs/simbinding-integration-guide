pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
// Private credentials live in credentials.properties (git-ignored).
val credentialsProperties = java.util.Properties().apply {
    val file = rootDir.resolve("credentials.properties")
    require(file.exists()) {
        "Missing ${file.absolutePath}. Create it with artifactory.username and artifactory.password."
    }
    file.inputStream().use { load(it) }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            url = uri("https://sign3.jfrog.io/artifactory/intelligence-generic-local/")
            credentials {
                username = credentialsProperties.getProperty("artifactory.username")
                password = credentialsProperties.getProperty("artifactory.password")
            }
        }
    }
}

rootProject.name = "Sim Binding Integration"
include(":app")
 