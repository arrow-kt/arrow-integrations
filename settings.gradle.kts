enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

plugins {
  id("com.gradle.develocity") version "3.19"
  id("org.gradle.toolchains.foojay-resolver-convention") version("0.9.0")
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("gradle/projects.libs.versions.toml"))
      val kotlinVersion: String? by settings
      kotlinVersion?.let { version("kotlin", it) }
    }
  }

  repositories {
    mavenCentral()
  }
}

rootProject.name = "arrow-integrations"

include(":arrow-integrations-jackson-module")
// include("arrow-integrations-retrofit-adapter")
