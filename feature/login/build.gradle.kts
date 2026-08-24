plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.bsdevs.login"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:authentication"))
    implementation(project(":core:common"))
    implementation(project(":core:common:uicomponents"))
}
