import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use(::load)
        }
    }

fun configProvider(propertyName: String, environmentName: String) =
    providers.gradleProperty(propertyName)
        .orElse(
            localProperties.getProperty(propertyName)?.trim()?.takeUnless { it.isNullOrEmpty() }
                ?.let { providers.provider { it } }
                ?: providers.environmentVariable(environmentName).map(String::trim),
        )

fun String.toBuildConfigString(): String =
    buildString(length + 2) {
        append('"')
        this@toBuildConfigString.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(character)
            }
        }
        append('"')
    }

val azureAppIdProvider = configProvider("azureAppId", "AZURE_APP_ID")
val azureTenantIdProvider = configProvider("azureTenantId", "AZURE_TENANT_ID")
val graphBaseUrlProvider =
    configProvider("graphBaseUrl", "GRAPH_BASE_URL")
        .orElse("https://graph.microsoft.com/v1.0/")
val graphScopesProvider =
    configProvider("graphScopes", "GRAPH_SCOPES")
        .orElse("Calendars.Read")

val msalConfigTemplate = layout.projectDirectory.file("msal_config.json.template")
val generatedMsalResDir = layout.buildDirectory.dir("generated/res/msal")

val generateMsalConfig by tasks.registering {
    val outputFile = generatedMsalResDir.map { it.file("raw/msal_config.json") }

    inputs.file(msalConfigTemplate)
    inputs.property("azureAppId", azureAppIdProvider.orNull ?: "")
    inputs.property("azureTenantId", azureTenantIdProvider.orNull ?: "")
    outputs.file(outputFile)

    doLast {
        val azureAppId = azureAppIdProvider.orNull?.trim().orEmpty()
        val azureTenantId = azureTenantIdProvider.orNull?.trim().orEmpty()
        if (azureAppId.isBlank()) {
            logger.warn("Azure App ID is not configured. Set azureAppId or AZURE_APP_ID before signing in.")
        }

        val audienceType = if (azureTenantId.isBlank()) "AzureADMultipleOrgs" else "AzureADMyOrg"
        val tenantJsonLine =
            if (azureTenantId.isBlank()) {
                ""
            } else {
                ",\n        \"tenant_id\": \"$azureTenantId\""
            }

        val renderedConfig =
            msalConfigTemplate.asFile.readText()
                .replace("__AZURE_APP_ID__", azureAppId.ifBlank { "YOUR_AZURE_AD_CLIENT_ID" })
                .replace("__AZURE_AUTHORITY_AUDIENCE_TYPE__", audienceType)
                .replace("__AZURE_TENANT_ID_JSON__", tenantJsonLine)

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(renderedConfig)
    }
}

android {
    namespace = "com.g365calendar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.g365calendar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            "String",
            "GRAPH_BASE_URL",
            graphBaseUrlProvider.get().toBuildConfigString(),
        )
        buildConfigField(
            "String",
            "GRAPH_SCOPES",
            graphScopesProvider.get().toBuildConfigString(),
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        named("main") {
            res.srcDir(generatedMsalResDir)
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // MSAL
    implementation(libs.msal) {
        // `display-mask` is resolved from a private Azure feed in some MSAL transitive chains.
        // Exclude it to keep builds reproducible without Azure Artifacts credentials.
        exclude(group = "com.microsoft.device.display", module = "display-mask")
    }

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // WorkManager
    implementation(libs.workmanager)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Logging
    implementation(libs.timber)

    // Garmin Connect IQ Mobile SDK
    implementation(libs.garmin.connectiq)

    // Testing
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    if (name != "integrationTest") {
        useJUnitPlatform {
            excludeTags("integration")
        }
    }
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration-tagged Android JVM tests against Microsoft Graph."
    group = "verification"

    val debugUnitTest = tasks.named<Test>("testDebugUnitTest")

    dependsOn("compileDebugUnitTestSources")
    testClassesDirs = debugUnitTest.get().testClassesDirs
    classpath = debugUnitTest.get().classpath
    shouldRunAfter(debugUnitTest)

    useJUnitPlatform {
        includeTags("integration")
    }

    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

tasks.named("preBuild") {
    dependsOn(generateMsalConfig)
}
