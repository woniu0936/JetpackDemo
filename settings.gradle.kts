pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

rootProject.name = "JetpackDemo"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":core:common")
include(":core:datastore")
include(":core:datastore-proto")
include(":core:datastore-json-ktx")
include(":core:datastore-json-gson")
include(":core:datastore-parcelable")
include(":core:mmkv")
include(":core:logger")
include(":core:crash")
include(":core:view")
include(":core:compose")
