plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.bsdevs.forms"
    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:common:uicomponents"))

    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
