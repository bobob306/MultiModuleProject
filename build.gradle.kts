// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    id("com.google.dagger.hilt.android") version "2.54" apply false // Hilt plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false // Kotlin serialization plugin
}