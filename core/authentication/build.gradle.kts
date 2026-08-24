plugins {
    id("mmp.android.library")
    id("mmp.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bsdevs.authentication"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
}
