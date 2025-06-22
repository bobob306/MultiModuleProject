plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt") // Use kapt for Hilt
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" // Apply the plugin
    id("kotlin-parcelize")
}

android {
    namespace = "com.bsdevs.coffee"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:authentication"))
    implementation(project(":core:common:uicomponents"))
    debugImplementation(libs.androidx.compose.ui.ui.tooling)

    kapt(libs.hilt.compiler) // Use kapt for Hilt
    implementation(libs.androidx.hilt.navigation.compose) // For Hilt with Navigation Compose
    implementation(libs.androidx.navigation.compose) // Or latest
    implementation(libs.androidx.lifecycle.runtime.ktx) // Or latest
    implementation(libs.androidx.activity.compose) // Or latest
    implementation(platform(libs.androidx.compose.bom)) // Or latest
    implementation(libs.androidx.ui) // Or latest
    implementation(libs.androidx.ui.graphics) // Or latest
    implementation(libs.androidx.ui.tooling.preview) // Or latest
    implementation(libs.androidx.material3) // Or latest
    implementation(libs.hilt.android) // Hilt compiler
    implementation(libs.kotlinx.serialization.json) // Or latest version
    implementation(libs.kotlinx.coroutines.android) // Or latest version
    implementation(libs.kotlinx.coroutines.core) // Or latest version
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)
}