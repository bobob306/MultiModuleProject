plugins {
    id("mmp.android.library")
    id("mmp.android.hilt")
}

android {
    namespace = "com.bsdevs.data"
}

dependencies {
    implementation(project(":core:network"))
}
