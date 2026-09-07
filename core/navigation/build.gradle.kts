plugins {
    id("mmp.android.library")
    id("mmp.android.hilt")
    id("mmp.android.compose")
}

android {
    namespace = "com.bsdevs.navigation"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":feature:home"))
    implementation(project(":feature:coffee"))
    implementation(project(":feature:login"))
    implementation(project(":feature:splashscreen"))
    implementation(project(":feature:babycare"))
    implementation(project(":feature:forms"))
    implementation(project(":core:common:uicomponents"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
