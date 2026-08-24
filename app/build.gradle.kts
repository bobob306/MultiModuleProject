plugins {
    id("mmp.android.application")
    id("mmp.android.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
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
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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
    implementation(project(":feature:home"))
    implementation(project(":feature:babycare"))
    implementation(project(":core:authentication"))
    implementation(project(":feature:coffee"))
    implementation(project(":feature:splashscreen"))
    implementation(project(":feature:login"))

    ksp(libs.hilt.compiler)
    
    implementation(libs.androidx.appfunctions.common)
    ksp(libs.androidx.appfunctions.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.base)

    debugImplementation(libs.androidx.ui.test.manifest)
}
