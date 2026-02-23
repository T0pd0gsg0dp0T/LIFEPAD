import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dependencycheck)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore = keystorePropertiesFile.exists().also { exists ->
    if (exists) {
        FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
    }
}
val nvdApiKey = System.getenv("NVD_API_KEY")

android {
    namespace = "com.lifepad.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lifepad.app"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.lifepad.app.LifepadTestRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                // Fallback for local installs; CI/prod should provide keystore.properties
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    failBuildOnCVSS = 7.0f
    autoUpdate = !nvdApiKey.isNullOrBlank()
    failOnError = false
    skip = nvdApiKey.isNullOrBlank()
    nvd(
        closureOf<org.owasp.dependencycheck.gradle.extension.NvdExtension> {
            apiKey = nvdApiKey
        }
    )
}

tasks.register("connectedAndroidTestKeepApp") {
    group = "verification"
    description = "Runs connectedAndroidTest and reinstalls debug APK afterward so it stays on device."
    dependsOn("connectedAndroidTest")
    finalizedBy("installDebug")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Markdown
    implementation(libs.bundles.markwon)

    // Charts
    implementation(libs.mpandroidchart)

    // WorkManager
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Security
    implementation(libs.sqlcipher)
    implementation(libs.security.crypto)
    implementation(libs.lifecycle.process)
    compileOnly(libs.errorprone.annotations)

    // Unit Testing
    testImplementation(libs.bundles.testing)

    // Instrumented Testing
    androidTestImplementation(libs.bundles.android.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
