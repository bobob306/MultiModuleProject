plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // Kapt for Hilt annotation processing
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" // Apply the plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.bsdevs.multimoduleproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bsdevs.multimoduleproject"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SIGN_STORE_FILE") ?: project.findProperty("storeFile") ?: "defaultKeystore")
            storePassword = System.getenv("SIGN_STORE_PASSWORD") ?: project.findProperty("storePassword") as String?
            keyAlias = System.getenv("SIGN_KEY_ALIAS") ?: project.findProperty("keyAlias") as String?
            keyPassword = System.getenv("SIGN_KEY_PASSWORD") ?: project.findProperty("keyPassword") as String?
        }
    }

    buildTypes {

        release {
            signingConfig = signingConfigs.getByName("release")
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
    packaging {
        resources.excludes.add("META-INF/gradle/incremental.annotation.processors")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(project(":core:navigation"))
    implementation(project(":feature:home"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.kotlinx.serialization.json) // Or latest version
    implementation(libs.kotlinx.coroutines.android) // Or latest version
    implementation(libs.kotlinx.coroutines.core) // Or latest version

    implementation(libs.hilt.android) // Hilt
    implementation(libs.androidx.hilt.navigation.compose) // Hilt navigation
    implementation(libs.hilt.android.compiler) // Hilt compiler
    kapt(libs.hilt.compiler) // Hilt compiler

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.play.services.base)
}