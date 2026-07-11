pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://qisdk.softbankrobotics.com/sdk/maven")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://qisdk.softbankrobotics.com/sdk/maven")
    }
}

rootProject.name = "Pepper-Presenter"
include(":app")
