pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HybridModeApp"

// Android: :app, :core:* | Backend: backend/ | Config: config.properties
val configFile = file("config.properties")
val exampleFile = file("config.properties.example")
if (!configFile.exists()) {
    println(
        """
        |
        |[HybridMode] Missing config.properties
        |  → cp config.properties.example config.properties
        |  → edit STRAPI_BASE_URL for this laptop / real device IP
        |  Falling back to config.properties.example defaults.
        |
        """.trimMargin()
    )
    if (!exampleFile.exists()) {
        throw GradleException(
            "Neither config.properties nor config.properties.example found. " +
                "Add config.properties.example to the repo root."
        )
    }
}

include(":app")
include(":core:common")
include(":core:domain")
include(":core:data")
