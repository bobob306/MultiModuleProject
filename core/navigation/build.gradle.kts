plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt") // Use kapt for Hilt
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bsdevs.navigation"
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(project(":feature:homescreen"))
    implementation(project(":feature:coffeescreen"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.hilt.android) // Or latest
    kapt(libs.hilt.compiler) // Use kapt for Hilt
    implementation(libs.androidx.hilt.navigation.compose) // For Hilt with Navigation Compose

    implementation(libs.androidx.navigation.compose) // Or latest

    implementation(libs.androidx.lifecycle.runtime.ktx) // Or latest
    implementation(libs.androidx.activity.compose) // Or latest
    implementation(platform(libs.androidx.compose.bom)) // Or latest
    implementation(libs.androidx.ui) // Or latest
    implementation(libs.androidx.ui.graphics) // Or latest
    implementation(libs.androidx.ui.tooling.preview) // Or latest
    implementation(libs.androidx.ui.tooling) // Or latest
    implementation(libs.androidx.material3.android) // Or latest
}