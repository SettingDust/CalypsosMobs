@file:Suppress("UnstableApiUsage")

import earth.terrarium.cloche.IncludeTransformationState
import groovy.lang.Closure


plugins {
    java

    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"

    id("com.palantir.git-version") version "3.1.0"

    id("com.gradleup.shadow") version "8.3.6"

    id("earth.terrarium.cloche") version "0.11.7"
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
            version {
                start = "1.20.1"
            }
        }

        dependency {
            modId = "geckolib"
        }
    }

    mappings {
        official()
    }

    common {
        mixins.from(file("src/common/main/resources/$id.mixins.json"))

        dependencies {
            compileOnly("org.spongepowered:mixin:0.8.7")
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
            implementation("io.github.llamalad7:mixinextras-forge:0.5.0-rc.4") {
                attributes {
                    attribute(IncludeTransformationState.ATTRIBUTE, IncludeTransformationState.Extracted)
                }
            }

            modImplementation("thedarkcolour:kotlinforforge:4.11.0")

            modImplementation("software.bernie.geckolib:geckolib-forge-1.20.1:4.7.3") {
                attributes {
                    attribute(IncludeTransformationState.ATTRIBUTE, IncludeTransformationState.Extracted)
                }
            }
        }
    }

    targets.all {
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

kotlin {
    jvmToolchain(17)
}