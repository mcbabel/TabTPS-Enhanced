enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
  repositories {
    mavenCentral {
      mavenContent { releasesOnly() }
    }
    maven("https://repo.jpenilla.xyz/snapshots/") {
      mavenContent {
        snapshotsOnly()
        includeGroup("xyz.jpenilla")
        includeGroup("org.incendo")
      }
    }
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      mavenContent { snapshotsOnly() }
    }
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/") {
      mavenContent { includeGroup("me.clip") }
    }
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.spongepowered.org/repository/maven-snapshots/") {
      mavenContent { snapshotsOnly() }
    }
    extensions.getByType<net.minecraftforge.gradle.MinecraftExtension>().mavenizer(this)
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
  }
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.jpenilla.xyz/snapshots/")
  }
  includeBuild("gradle/build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("net.minecraftforge.gradle") version "7.0.40"
}

rootProject.name = "TabTPS"

listOf(
  "common",
  "folia",
).forEach { module ->
  include("tabtps-$module")
  project(":tabtps-$module").projectDir = file(module)
}

listOf(
  "1.20.1",
  "26.3",
).forEach { version ->
  val name = "tabtps-forge-${version.replace('.', '-')}"
  include(name)
  project(":$name").projectDir = file("forge/$version")
}
