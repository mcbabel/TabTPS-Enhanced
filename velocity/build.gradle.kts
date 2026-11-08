plugins {
  id("tabtps.platform")
}

indra {
  javaVersions {
    target(17)
    minimumToolchain(25)
  }
}

dependencies {
  compileOnly(libs.velocityApi)
}

tasks {
  jar {
    archiveFileName.set("tabtps-velocity-${project.version}.jar")
    from(rootProject.file("license.txt")) {
      rename { "license_tabtps.txt" }
    }
  }

  processResources {
    val replacements = mapOf(
      "version" to version.toString(),
      "description" to project.description,
      "github" to "https://github.com/MCbabel/TabTPS-Enhanced"
    )
    inputs.properties(replacements)
    filesMatching("velocity-plugin.json") {
      expand(replacements)
    }
  }
}

tabTPSPlatform {
  productionJar.set(tasks.jar.flatMap { it.archiveFile })
}

publishMods.displayName.set("TabTPS Enhanced ${project.version} (Velocity)")
publishMods.version.set("${project.version}+velocity")

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
  modLoaders.add("velocity")
}
