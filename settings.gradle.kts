pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.9.1"
        id("org.jetbrains.kotlin.android") version "2.1.0"
        id("org.jetbrains.kotlin.jvm") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PflegeFuerAnfaenger"

include(":core")
// Ohne Android-SDK (z.B. in reinen JVM-Umgebungen) lässt sich nur :core bauen und testen:
//   ./gradlew -PcoreOnly :core:test
if (!providers.gradleProperty("coreOnly").isPresent) {
    include(":app")
}
