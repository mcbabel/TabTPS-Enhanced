import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment

plugins {
  id("tabtps.platform.shadow")
  id("net.minecraftforge.gradle")
  id("net.minecraftforge.renamer") version "1.1.2"
}

val minecraftVersion = "1.20.1"

indra {
  javaVersions {
    target(17)
    minimumToolchain(25)
  }
}

val shade: Configuration = configurations.create("shade")

configurations.implementation {
  extendsFrom(shade)
}

minecraft {
  mappings("official", minecraftVersion)
  runs {
    register("server") {
      args("--nogui")
    }
    register("client")
  }
}

dependencies {
  implementation(minecraft.dependency("net.minecraftforge:forge:${libs.versions.forge1201.get()}"))
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
    archiveClassifier.set("mojmap")
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

val renameShadowJar = renamer.classes("renameShadowJar", tasks.shadowJar) {
  map.from(minecraft.dependency.toSrgFile)
  output.set(layout.buildDirectory.file("libs/tabtps-forge-mc$minecraftVersion-${project.version}.jar"))
}

tasks.assemble {
  dependsOn(renameShadowJar)
}

tabTPSPlatform {
  productionJar.set(renameShadowJar.flatMap { it.output })
}

publishMods.displayName.set("TabTPS Enhanced ${project.version} (Forge $minecraftVersion)")
publishMods.version.set("${project.version}+forge-$minecraftVersion")

publishMods.modrinth {
  environment = ModrinthEnvironment.SERVER_ONLY
  minecraftVersions.add(minecraftVersion)
  modLoaders.add("forge")
}
