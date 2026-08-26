// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false // Hilt plugin
    alias(libs.plugins.kotlin.serialization) apply false // Kotlin serialization plugin
    id("com.google.gms.google-services") version "4.4.2" apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
}
