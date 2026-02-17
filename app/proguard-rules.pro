# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Hilt/Dagger
-keep class com.rajatt7z.retailx.ReTailX { *; }
-keep class com.rajatt7z.retailx.di.** { *; }
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keep public class * extends dagger.hilt.internal.GeneratedComponent

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keep class com.google.gson.** { *; }
-keep class com.rajatt7z.retailx.models.** { *; }

# OkHttp
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Firebase
-keep class com.google.firebase.** { *; }

# Android Components
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }