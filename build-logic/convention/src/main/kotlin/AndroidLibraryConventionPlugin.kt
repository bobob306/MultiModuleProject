import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<LibraryExtension> {
                compileSdk = 37
                defaultConfig {
                    minSdk = 26
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    consumerProguardFiles("consumer-rules.pro")
                }

                testOptions {
                    unitTests {
                        isReturnDefaultValues = true
                        all {
                            it.maxHeapSize = "2g"
                            it.forkEvery = 10
                            it.maxParallelForks = 1 // Prevent OOM by running tests sequentially
                        }
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            dependencies {
                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("androidx-appcompat").get())
                add("implementation", libs.findLibrary("material").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
                
                add("testImplementation", libs.findLibrary("junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add("androidTestImplementation", libs.findLibrary("androidx-espresso-core").get())
            }
        }
    }
}
