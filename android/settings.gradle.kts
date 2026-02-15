pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://pkgs.dev.azure.com/AzureAD/AuthClient/_packaging/AuthClientRelease/maven/v1") }
    }
}

rootProject.name = "G365Calendar"
include(":app")
