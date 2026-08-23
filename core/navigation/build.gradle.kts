plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bsdevs.navigation"
    compileSdk = 37

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(project(":feature:home"))
    implementation(project(":feature:coffee"))
    implementation(project(":feature:login"))
    implementation(project(":feature:splashscreen"))
    implementation(project(":feature:babycare"))
    implementation(project(":core:common:uicomponents"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.hilt.android) // Or latest
    ksp(libs.hilt.compiler) // Use KSP for Hilt
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
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.appfunctions.common)
    // KSP Compiler compiler targeting tool schemas
    ksp(libs.androidx.appfunctions.compiler)
}
