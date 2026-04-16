pluginManagement {
    repositories {
        google { content { includeGroupByPrefix("com.android"); includeGroupByPrefix("com.google"); includeGroupByPrefix("androidx") } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "OfflineAI"
include(":app")
