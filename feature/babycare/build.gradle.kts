plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.google.ksp)
}

android {
    namespace = "com.bsdevs.babycare"
}

ksp {
    arg("androidx.appfunctions.generateSelfDescribingConfig", "true")
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:renderer"))
    implementation(project(":core:authentication"))
    
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    implementation(libs.androidx.appfunctions.common)
    ksp(libs.androidx.appfunctions.compiler)
}
