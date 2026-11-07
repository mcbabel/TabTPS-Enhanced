import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment

plugins {
  id("tabtps.platform.shadow")
  id("net.minecraftforge.gradle")
}

val minecraftVersion = "26.3"

indra {
  javaVersions {
    target(25)
    minimumToolchain(25)
  }
}

val shade: Configuration = configurations.create("shade")

configurations.implementation {
  extendsFrom(shade)
}

minecraft {
  runs {
    register("server") {
      args("--nogui")
    }
    register("client")
  }
}

dependencies {
  implementation(minecraft.dependency("net.minecraftforge:forge:${libs.versions.forge263.get()}"))
  shade(projects.tabtpsCommon)
  shade(libs.cloudBrigadier)
  shade(libs.adventureTextSerializerGson)
}

tasks {
  jar {
    archiveClassifier.set("unshaded")
  }

  shadowJar {
    configurations = listOf(shade)
    dependencies {
      exclude(dependency("com.google.code.gson:.*"))
      exclude(dependency("com.google.guava:.*"))
      exclude(dependency("com.google.errorprone:.*"))
      exclude(dependency("com.mojang:.*"))
      exclude(dependency("org.slf4j:.*"))
    }
    archiveFileName.set("tabtps-forge-mc$minecraftVersion-${project.version}.jar")
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
    sequenceOf(
      "org.incendo.cloud",
      "io.leangen.geantyref",
      "net.kyori",
      "org.spongepowered.configurate",
      "org.checkerframework",
      "org.immutables",
    ).forEach { pkg ->
      relocate(pkg, "${rootProject.group}.${rootProject.name.lowercase()}.lib.$pkg")
    }
  }

  processResources {
    val replacements = mapOf(
      "version" to version.toString(),
      "description" to project.description,
      "minecraftVersion" to minecraftVersion,
    )
    inputs.properties(replacements)
    filesMatching("META-INF/mods.toml") {
      expand(replacements)
    }
    from(rootProject.file("assets/logo-128.png")) {
      rename { "tabtps.png" }
    }
  }
}

tabTPSPlatform {
  productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}

publishMods.displayName.set("TabTPS Folia ${project.version} (Forge $minecraftVersion)")
publishMods.version.set("${project.version}+forge-$minecraftVersion")

publishMods.modrinth {
  environment = ModrinthEnvironment.SERVER_ONLY
  minecraftVersions.add(minecraftVersion)
  modLoaders.add("forge")
}
