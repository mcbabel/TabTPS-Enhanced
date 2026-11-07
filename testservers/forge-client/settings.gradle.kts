plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("net.minecraftforge.gradle") version "7.0.40"
}

dependencyResolutionManagement {
  repositories {
    extensions.getByType<net.minecraftforge.gradle.MinecraftExtension>().mavenizer(this)
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
    mavenCentral()
  }
}

rootProject.name = "tabtps-forge-client"
