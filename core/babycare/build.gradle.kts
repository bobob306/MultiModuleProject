plugins {
    id("mmp.android.library")
    id("mmp.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bsdevs.babycare.core"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.play.services)
}
