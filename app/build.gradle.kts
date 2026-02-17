import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    compileSdk = 36

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
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures{
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // Allow Hilt to use Java 8 features
}

dependencies {

    implementation(libs.shimmer)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation("com.google.firebase:firebase-crashlytics:19.4.0")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.mpandroidchart)
    implementation(libs.coil)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // AI
    implementation(libs.generativeai)

    //anim
    implementation(libs.lottie)

    // Pull-to-Refresh
    implementation(libs.swiperefreshlayout)

    // Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.guava:guava:32.1.3-android")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Unit Testing
    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("androidx.arch.core:core-testing:2.2.0") // Keep hardcoded if not in libs
}