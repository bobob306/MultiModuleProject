plugins {
    id("mmp.android.library")
    id("mmp.android.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.bsdevs.navigation"
}

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":feature:coffee"))
    implementation(project(":feature:login"))
    implementation(project(":feature:splashscreen"))
    implementation(project(":feature:babycare"))
    implementation(project(":core:common:uicomponents"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.appfunctions.common)
    ksp(libs.androidx.appfunctions.compiler)
}
