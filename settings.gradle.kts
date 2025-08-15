pluginManagement {
    repositories {
        google { // This is the unrestricted one that resolves the dependency
            // All Google-hosted libraries will be available here
        }
        google { // Your original restricted block
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "petCare"
include(":app")