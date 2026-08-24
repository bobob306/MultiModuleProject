plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization) // Apply the plugin
    id("kotlin-parcelize")
}

android {
    namespace = "com.bsdevs.home"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:renderer"))
    implementation(project(":core:common:uicomponents"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
}
