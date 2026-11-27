pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven ( url = "https://jitpack.io" )
        maven ( url = "https://nexus-external.rustore.ru/repository/maven-rustore-exposed" )
    }
}

rootProject.name = "f1app"
include(":app")
