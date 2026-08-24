plugins {
    id("mmp.android.library")
    id("mmp.android.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.bsdevs.renderer"
}

dependencies {
    implementation(project(":core:data"))

    implementation(libs.coil.compose)
    implementation(libs.androidx.runtime.android)
}
