plugins {
    id("mmp.android.library")
    id("mmp.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bsdevs.network"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)

    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.retrofit)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
