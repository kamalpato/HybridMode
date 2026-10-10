import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Keys from repo-root config.properties (see config.properties.example).
val hybridConfig: Properties = Properties().apply {
    val local = rootProject.file("config.properties")
    val example = rootProject.file("config.properties.example")
    when {
        local.exists() -> local.inputStream().use { load(it) }
        example.exists() -> example.inputStream().use { load(it) }
    }
}

fun hybridString(key: String, default: String): String =
    hybridConfig.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: default

fun hybridBoolean(key: String, default: Boolean): Boolean =
    hybridString(key, default.toString()).equals("true", ignoreCase = true)

fun hybridLong(key: String, default: Long): Long =
    hybridString(key, default.toString()).toLongOrNull() ?: default

fun quoted(value: String): String = "\"${value.replace("\"", "\\\"")}\""

val strapiBaseUrl = hybridString("STRAPI_BASE_URL", "http://10.0.2.2:1337/")
    .let { if (it.endsWith("/")) it else "$it/" }
val releaseBaseUrl = hybridString("RELEASE_BASE_URL", strapiBaseUrl)
    .let { if (it.endsWith("/")) it else "$it/" }
val useMockBackend = hybridBoolean("USE_MOCK_BACKEND", false)
val accessTokenTtlMs = hybridLong("ACCESS_TOKEN_TTL_MS", 300_000L)

android {
    namespace = "com.hybridmode.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "BASE_URL", quoted(strapiBaseUrl))
        buildConfigField("boolean", "USE_MOCK_BACKEND", useMockBackend.toString())
        buildConfigField("long", "ACCESS_TOKEN_TTL_MS", "${accessTokenTtlMs}L")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", quoted(strapiBaseUrl))
            buildConfigField("boolean", "USE_MOCK_BACKEND", useMockBackend.toString())
        }
        release {
            buildConfigField("String", "BASE_URL", quoted(releaseBaseUrl))
            buildConfigField("boolean", "USE_MOCK_BACKEND", "false")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

logger.lifecycle(
    "[HybridMode] config → BASE_URL=$strapiBaseUrl | USE_MOCK_BACKEND=$useMockBackend | TTL=${accessTokenTtlMs}ms"
)
