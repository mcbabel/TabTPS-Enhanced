plugins {
  id("tabtps.platform.shadow")
  alias(libs.plugins.hangarPublish)
}

indra {
  javaVersions {
    target(17)
    minimumToolchain(25)
  }
}

dependencies {
  implementation(projects.tabtpsCommon)

  compileOnly(libs.foliaApi) {
    exclude(group = "net.kyori")
    exclude(group = "org.slf4j")
  }
  compileOnly(libs.brigadier)
  compileOnly(libs.placeholderApi) {
    isTransitive = false
  }
  implementation(libs.adventurePlatformBukkit)
  implementation(libs.adventureTextSerializerPlain)
  implementation(libs.bstatsBukkit)
  implementation(libs.slf4jJdk14)

  implementation(libs.cloudPaper)
}

base {
  archivesName.set("tabtps-enhanced")
}

tasks {
  jar {
    archiveClassifier.set("unshaded")
  }

  shadowJar {
    archiveClassifier.set(null as String?)
    sequenceOf(
      "org.slf4j",
      "org.incendo.cloud",
      "io.leangen.geantyref",
      "net.kyori",
      "org.spongepowered.configurate",
      "org.checkerframework",
      "org.bstats",
      "xyz.jpenilla.pluginbase"
    ).forEach { pkg ->
      relocate(pkg, "${rootProject.group}.${rootProject.name.lowercase()}.lib.$pkg")
    }
    manifest {
      attributes("paperweight-mappings-namespace" to "mojang")
    }
  }

  processResources {
    val replacements = mapOf(
      "version" to version.toString(),
      "description" to project.description,
      "github" to "https://github.com/MCbabel/TabTPS-Enhanced"
    )
    inputs.properties(replacements)
    filesMatching("plugin.yml") {
      expand(replacements)
    }
  }
}

tabTPSPlatform {
  productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}

publishMods.modrinth {
  minecraftVersions.addAll(
    "1.20",
    "1.20.1",
    "1.20.2",
    "1.20.3",
    "1.20.4",
    "1.20.5",
    "1.20.6",
    "1.21",
    "1.21.1",
    "1.21.2",
    "1.21.3",
    "1.21.4",
    "1.21.5",
    "1.21.6",
    "1.21.7",
    "1.21.8",
    "1.21.9",
    "1.21.10",
    "1.21.11",
    "26.1",
    "26.1.1",
    "26.1.2",
    "26.2",
    "26.3"
  )
  modLoaders.add("folia")
  modLoaders.add("paper")
  modLoaders.add("purpur")
  modLoaders.add("spigot")
}

val paperVersions = listOf(
  "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
  "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
  "26.1", "26.1.1", "26.1.2", "26.2", "26.3"
)

hangarPublish {
  publications.register("plugin") {
    version.set(project.version as String)
    id.set("TabTPS-Enhanced")
    channel.set("Release")
    changelog.set(releaseNotes)
    apiKey.set(providers.environmentVariable("HANGAR_TOKEN"))
    platforms {
      paper {
        jar.set(tasks.shadowJar.flatMap { it.archiveFile })
        platformVersions.set(paperVersions)
      }
      velocity {
        jar.set(rootProject.layout.projectDirectory.file("velocity/build/libs/tabtps-velocity-${project.version}.jar"))
        platformVersions.set(listOf("3.4", "3.5", "4.0.0", "4.1.0", "4.1.1", "4.2.0", "4.2.1"))
      }
    }
  }
}

tasks.matching { it.name.startsWith("publish") && it.name.endsWith("ToHangar") }.configureEach {
  dependsOn(tasks.shadowJar, ":tabtps-velocity:jar")
}
