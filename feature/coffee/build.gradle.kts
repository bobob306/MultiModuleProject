plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.bsdevs.coffee"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:authentication"))
    implementation(project(":core:common:uicomponents"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
