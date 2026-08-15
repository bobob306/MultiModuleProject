plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization) // Apply the plugin
    id("kotlin-parcelize")
}

android {
    namespace = "com.bsdevs.home"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:renderer"))
    ksp(libs.hilt.compiler) // Use KSP for Hilt
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

}