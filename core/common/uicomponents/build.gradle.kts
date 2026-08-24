plugins {
    id("mmp.android.library")
    id("mmp.android.compose")
}

android {
    namespace = "com.bsdevs.uicomponents"
}

dependencies {
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
