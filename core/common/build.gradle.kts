plugins {
    id("mmp.android.library")
}

android {
    namespace = "com.bsdevs.common"
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
}
