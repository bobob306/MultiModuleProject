plugins {
    id("mmp.android.library")
    id("mmp.android.hilt")
}

android {
    namespace = "com.bsdevs.common"
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.junit)
}
