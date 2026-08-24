plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.myapplication.priceinput"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
}
