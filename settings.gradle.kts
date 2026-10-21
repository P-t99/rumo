pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.android.")) useModule("com.android.tools.build:gradle:${requested.version}")
        }
    }
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Rumo"
include(":app", ":floatingx-core", ":floatingx-system")
project(":floatingx-core").projectDir = file("vendor/floatingx-core")
project(":floatingx-system").projectDir = file("vendor/floatingx-system")
