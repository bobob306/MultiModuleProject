plugins {
    id("mmp.android.feature")
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
}

android {
    namespace = "com.bsdevs.babycare"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:babycare"))
    implementation(project(":core:renderer"))
    implementation(project(":core:authentication"))
    implementation(project(":core:common:uicomponents"))
    
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}

android {
    testOptions {
        unitTests {
            all {
                it.maxHeapSize = "1g"
                it.forkEvery = 1 // Every test gets a fresh JVM to prevent MockK metadata buildup
                // Add some diagnostics to see if workers are dying
                it.testLogging.showStandardStreams = true
            }
        }
    }
}
