pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MultiModuleProject"
include(":app")
include(":core")
include(":core:navigation")
include(":feature")
include(":feature:splashscreen")
include(":feature:home")
include(":core:common")
include(":core:network")
include(":core:data")
include(":core:renderer")
include(":feature:priceinput")
include(":feature:coffee")
include(":core:authentication")
include(":feature:login")
include(":core:common:uicomponents")
include(":feature:babycare")
