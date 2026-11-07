plugins {
  java
  id("net.minecraftforge.gradle")
}

val forgeVersion = providers.gradleProperty("forgeVersion").getOrElse("26.3-66.1.5")
val javaVersion = providers.gradleProperty("javaVersion").getOrElse("25").toInt()
val runDir = layout.projectDirectory.dir("run/$forgeVersion")

java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

minecraft {
  providers.gradleProperty("mappings").orNull?.let { mappings("official", it) }
  runs {
    register("client") {
      workingDir = runDir
      args("--quickPlaySingleplayer", "tabtps-test")
    }
  }
}

dependencies {
  implementation(minecraft.dependency("net.minecraftforge:forge:$forgeVersion"))
}

tasks.matching { it.name == "runClient" }.configureEach {
  doFirst {
    layout.buildDirectory.dir("classes/java/main").get().asFile.mkdirs()
    layout.buildDirectory.dir("resources/main").get().asFile.mkdirs()
  }
}
