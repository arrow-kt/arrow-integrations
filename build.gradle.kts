import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.arrowGradleConfig.versioning)
  alias(libs.plugins.kotlin.binaryCompatibilityValidator)
}

allprojects {
  repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }

  group = property("projects.group").toString()
}

val jvmVersionTarget = 8

allprojects {
  tasks {
    withType<Test> {
      maxParallelForks = Runtime.getRuntime().availableProcessors()
      useJUnitPlatform()
      testLogging {
        setExceptionFormat("full")
        setEvents(listOf("passed", "skipped", "failed", "standardOut", "standardError"))
      }
    }

    withType<JavaCompile>().configureEach {
      sourceCompatibility = "${JavaVersion.toVersion(jvmVersionTarget)}"
      targetCompatibility = "${JavaVersion.toVersion(jvmVersionTarget)}"
    }

    withType<KotlinCompile>().configureEach {
      kotlinOptions {
        jvmTarget = "${JavaVersion.toVersion(jvmVersionTarget)}"
      }
    }
  }
}
