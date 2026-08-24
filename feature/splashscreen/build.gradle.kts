plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.bsdevs.splashscreen"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:authentication"))
}
