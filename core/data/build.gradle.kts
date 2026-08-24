plugins {
    id("mmp.android.library")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.bsdevs.data"
}

dependencies {
    implementation(project(":core:network"))
}
