plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.gms.google.services) apply false
    alias(libs.plugins.ksp) apply false
    id("androidx.navigation.safeargs.kotlin") version "2.9.7" apply false
    alias(libs.plugins.hilt.android) apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}