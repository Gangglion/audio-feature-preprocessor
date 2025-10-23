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

        maven(
            url = "https://mvn.0110.be/releases"
        ) {
            name = "TarsosDSP repository"
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven(
            url = "https://mvn.0110.be/releases"
        ) {
            name = "TarsosDSP repository"
        }
    }
}

rootProject.name = "AudioFeaturePreprocessor"
include(":app")
include(":audio-feature-preprocessor")
