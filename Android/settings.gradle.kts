pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // JitPack 저장소 추가 (필요한 경우)
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack 저장소 추가 (필요한 경우)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ARtravel"
include(":app")
// 123
