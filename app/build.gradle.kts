import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.gms.google.services)
    alias(libs.plugins.ksp)
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.hilt.android)
    id("com.google.firebase.crashlytics")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.rajatt7z.retailx"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rajatt7z.ReTailX"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_KEY", "\"${localProperties.getProperty("API_KEY")}\"")
        buildConfigField("String", "IMGBB_API_KEY", "\"${localProperties.getProperty("IMGBB_API_KEY")}\"")
    }

    buildTypes {
        getByName("debug") {
            ndk {
                // filter specifically for these ABIs to speed up the build and reduce the apk size
                // arm64-v8a: Most modern Android phones
                // x86_64: Most modern Emulators
                abiFilters.add("arm64-v8a")
                abiFilters.add("x86_64")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures{
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Core AndroidX & UI
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Jetpack Components (Lifecycle, Navigation, Room)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    ksp(libs.androidx.room.compiler)

    // Firebase (using BOM for version management)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Networking & Data
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // UI Components & Animations
    implementation(libs.coil)
    implementation(libs.lottie)
    implementation(libs.mpandroidchart)
    implementation(libs.shimmer)
    implementation(libs.swiperefreshlayout)

    // Location & Maps
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // AI & Machine Learning
    implementation(libs.generativeai)
    implementation(libs.mlkit.barcode.scanning)

    // Security & Utilities
    implementation(libs.androidx.biometric)
    implementation(libs.guava)

    // Database & Local Storage
    implementation(libs.bundles.room)
    implementation(libs.bundles.navigation)
    implementation(libs.bundles.camerax)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core.testing)

    // Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}