@file:Suppress("UnstableApiUsage")

import earth.terrarium.cloche.api.target.FabricTarget
import groovy.lang.Closure


plugins {
    java

    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"

    id("com.palantir.git-version") version "3.1.0"

    id("com.gradleup.shadow") version "8.3.6"

    id("earth.terrarium.cloche") version "0.11.21"
}

val archive_name: String by rootProject.properties
val id: String by rootProject.properties
val source: String by rootProject.properties

group = "settingdust.calypsos_mobs"

val gitVersion: Closure<String> by extra
version = gitVersion()

base { archivesName = archive_name }

repositories {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        content {
            includeGroup("thedarkcolour")
        }
    }

    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        content {
            includeGroupAndSubgroups("software.bernie")
            includeGroup("com.eliotlash.mclib")
        }
    }

    maven("https://dl.cloudsmith.io/public/tslat/sbl/maven/") {
        content {
            includeGroup("net.tslat.smartbrainlib")
        }
    }

    mavenCentral()

    cloche {
        librariesMinecraft()
        main()
        mavenFabric()
        mavenForge()
        mavenNeoforged()
        mavenNeoforgedMeta()
        mavenParchment()
    }

    mavenLocal()
}

cloche {
    metadata {
        modId = id
        name = rootProject.property("name").toString()
        description = rootProject.property("description").toString()
        license = "ARR"
        icon = "assets/$id/icon.png"
        sources = source
        issues = "$source/issues"
        author("SettingDust")

        dependency {
            modId = "minecraft"
            required = true
            version {
                start = "1.20.1"
            }
        }

        dependency {
            modId = "geckolib"
            required = true
        }

        dependency {
            modId = "smartbrainlib"
            required = true
        }
    }

    mappings {
        official()
    }

    common {
        mixins.from(file("src/common/main/resources/$id.mixins.json"))
        accessWideners.from(file("src/common/main/resources/$id.accesswidener"))

        dependencies {
            compileOnly("org.spongepowered:mixin:0.8.7")
        }
    }

    val commons = mapOf(
        "1.20.1" to common("common:1.20.1"),
        "1.21.1" to common("common:1.21.1"),
    )

    val fabricCommon = common("fabric:common") {
        mixins.from(file("src/fabric/common/main/resources/$id.fabric.mixins.json"))
    }

    targets.withType<FabricTarget> {
        dependsOn(fabricCommon)

        loaderVersion = "0.16.14"

        includedClient()

        metadata {
            entrypoint("main") {
                adapter = "kotlin"
                value = "settingdust.calypsos_mobs.fabric.Entrypoint::init"
            }

            entrypoint("client") {
                adapter = "kotlin"
                value = "settingdust.calypsos_mobs.fabric.Entrypoint::clientInit"
            }

            dependency {
                modId = "fabric-api"
            }

            dependency {
                modId = "fabric-language-kotlin"
            }
        }

        dependencies {
            modImplementation("net.fabricmc:fabric-language-kotlin:1.13.1+kotlin.2.1.10")
        }
    }

    fabric("fabric:1.20.1") {
        minecraftVersion = "1.20.1"

        dependencies {
            fabricApi("0.92.6")

            modImplementation(catalog.geckolib.get1().get20().get1().fabric)
            implementation(catalog.mclib)

            modImplementation(catalog.smartbrainlib.get1().get20().get1().fabric)
        }
    }

    fabric("fabric:1.21") {
        minecraftVersion = "1.21.1"

        dependencies {
            fabricApi("0.116.5")

            modImplementation(catalog.geckolib.get1().get21().get1().fabric)
            modImplementation(catalog.smartbrainlib.get1().get21().get1().fabric)
        }
    }

    forge {
        minecraftVersion = "1.20.1"
        loaderVersion = "47.4.4"

        metadata {
            modLoader = "kotlinforforge"
            loaderVersion {
                start = "4"
            }
        }

        repositories {
            maven("https://repo.spongepowered.org/maven") {
                content {
                    includeGroup("org.spongepowered")
                }
            }
        }

        dependencies {
            implementation("org.spongepowered:mixin:0.8.7")
            implementation(catalog.mixinextras.common)
            implementation(catalog.mixinextras.forge)

            modImplementation("thedarkcolour:kotlinforforge:4.11.0")

            modImplementation(catalog.geckolib.get1().get20().get1().forge)
            implementation(catalog.mclib)

            modImplementation(catalog.smartbrainlib.get1().get20().get1().forge)
        }
    }

    neoforge("neoforge:1.21") {
        minecraftVersion = "1.21.1"
        loaderVersion = "21.1.192"

        metadata {
            modLoader = "kotlinforforge"
            loaderVersion {
                start = "5"
            }
        }

        dependencies {
            modImplementation("thedarkcolour:kotlinforforge-neoforge:5.9.0")

            modImplementation(catalog.geckolib.get1().get21().get1().neoforge)
            modImplementation(catalog.smartbrainlib.get1().get21().get1().neoforge)
        }
    }

    targets.all {
        dependsOn(commons.getValue(minecraftVersion.get()))

        runs {
            client()
        }

        mappings {
            parchment(minecraftVersion.map {
                when (it) {
                    "1.20.1" -> "2023.09.03"
                    "1.21.1" -> "2024.11.17"
                    else -> throw IllegalArgumentException("Unsupported minecraft version $it")
                }
            })
        }
    }
}

tasks {
    withType<ProcessResources> {
        duplicatesStrategy = DuplicatesStrategy.WARN
    }
}