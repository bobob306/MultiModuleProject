plugins {
    id("mmp.android.library")
    id("mmp.android.hilt")
}

android {
    namespace = "com.bsdevs.forms.impl"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":feature:babycare"))
    implementation(project(":feature:coffee"))

    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
