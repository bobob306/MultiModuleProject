plugins {
    id("mmp.android.application")
    id("mmp.android.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.bsdevs.multimoduleproject"

    defaultConfig {
        applicationId = "com.bsdevs.multimoduleproject"
        versionCode = 1
        versionName = "1.0"
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
            // Use release signing if configured, otherwise fallback to debug signing
            signingConfig = if (System.getenv("SIGN_STORE_FILE") != null || project.hasProperty("storeFile")) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    packaging {
        resources.excludes.add("META-INF/gradle/incremental.annotation.processors")
    }
}

ksp {
    arg("androidx.appfunctions.generateSelfDescribingConfig", "true")
    arg("androidx.appfunctions.aggregateAppFunctions", "true")
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":feature:home"))
    implementation(project(":feature:babycare"))
    implementation(project(":core:authentication"))
    implementation(project(":feature:coffee"))
    implementation(project(":feature:splashscreen"))
    implementation(project(":feature:login"))
    implementation(project(":feature:forms"))

    ksp(libs.hilt.compiler)
    
    implementation(libs.androidx.appfunctions.common)
    ksp(libs.androidx.appfunctions.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.base)

    baselineProfile(project(":baselineprofile"))

    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
