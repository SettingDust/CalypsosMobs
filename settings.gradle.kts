enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    pluginManagement {
        repositories {
            mavenCentral()
            gradlePluginPortal()
            maven("https://maven.msrandom.net/repository/cloche")
            mavenLocal()
        }
    }
}

object VersionFormats {
    val versionPlusMc = { mcVer: String, ver: String -> "$ver+$mcVer" }
    val mcDashVersion = { mcVer: String, ver: String -> "$mcVer-$ver" }
}

object VersionTransformers {
    val versionDashLoader = { ver: String, loader: String -> "$ver-$loader" }
    val loaderUnderlineVersion = { ver: String, loader: String -> "${loader}_$ver" }
}

object ArtifactTransformers {
    val artifactDashLoaderDashMcVersion =
        { artifact: String, loader: String, mcVersion: String -> "$artifact-$loader-$mcVersion" }
    val artifactDashLoader = { artifact: String, loader: String, _: String -> "$artifact-$loader" }
}

open class LoaderConfig(
    val artifactTransformer: (artifact: String, loader: String, mcVersion: String) -> String = { artifact, _, _ -> artifact },
    val versionTransformer: (version: String, loader: String) -> String = { ver, _ -> ver }
) {
    companion object : LoaderConfig()
}

data class LoaderMapping(
    val mcVersion: String,
    val loaders: Map<String, LoaderConfig>
)

fun VersionCatalogBuilder.modrinth(
    id: String,
    artifact: String = id,
    mcVersionToVersion: Map<String, String>,
    versionFormat: (String, String) -> String = { _, v -> v },
    mapping: List<LoaderMapping> = emptyList()
) {
    val allLoaders = mapping.flatMap { it.loaders.keys }.toSet()
    val isSingleLoader = allLoaders.size == 1

    mcVersionToVersion.forEach { (mcVersion, modVersion) ->
        val config = mapping.find { it.mcVersion == mcVersion }
            ?: error("No loader config found for MC $mcVersion")

        val version = versionFormat(mcVersion, modVersion)

        config.loaders.forEach { (loaderName, loader) ->
            library(
                if (isSingleLoader) "${id}_${mcVersion}"
                else "${id}_${mcVersion}_$loaderName",
                "maven.modrinth",
                loader.artifactTransformer(artifact, loaderName, mcVersion)
            ).version(loader.versionTransformer(version, loaderName))
        }
    }
}

fun VersionCatalogBuilder.maven(
    id: String,
    group: String,
    artifact: String = id,
    mcVersionToVersion: Map<String, String>,
    versionFormat: (String, String) -> String = { _, v -> v },
    mapping: List<LoaderMapping> = emptyList()
) {
    val allLoaders = mapping.flatMap { it.loaders.keys }.toSet()
    val isSingleLoader = allLoaders.size == 1

    mcVersionToVersion.forEach { (mcVersion, baseVersion) ->
        val config = mapping.find { it.mcVersion == mcVersion }
            ?: error("No loader config found for MC $mcVersion")

        val version = versionFormat(mcVersion, baseVersion)

        config.loaders.forEach { (loaderName, loader) ->
            library(
                if (mcVersion == "*") {
                    if (isSingleLoader) id
                    else "${id}_$loaderName"
                } else {
                    if (isSingleLoader) "${id}_${mcVersion}"
                    else "${id}_${mcVersion}_$loaderName"
                },
                group,
                loader.artifactTransformer(artifact, loaderName, mcVersion)
            ).version(loader.versionTransformer(version, loaderName))
        }
    }
}

dependencyResolutionManagement.versionCatalogs.create("catalog") {
    maven(
        id = "mixinextras",
        group = "io.github.llamalad7",
        artifact = "mixinextras",
        mcVersionToVersion = mapOf("*" to "0.5.0"),
        versionFormat = { _, v -> v },
        mapping = listOf(
            LoaderMapping(
                "*", mapOf(
                    "forge" to LoaderConfig(ArtifactTransformers.artifactDashLoader),
                    "fabric" to LoaderConfig(ArtifactTransformers.artifactDashLoader),
                    "common" to LoaderConfig(ArtifactTransformers.artifactDashLoader)
                )
            )
        )
    )

    library("mclib", "com.eliotlash.mclib", "mclib").version("20")
    maven(
        id = "geckolib",
        group = "software.bernie.geckolib",
        artifact = "geckolib",
        mcVersionToVersion = mapOf(
            "1.20.1" to "4.7.3",
            "1.21.1" to "4.7.6"
        ),
        mapping = listOf(
            LoaderMapping(
                "1.20.1", mapOf(
                    "forge" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion),
                    "fabric" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion)
                )
            ),
            LoaderMapping(
                "1.21.1", mapOf(
                    "neoforge" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion),
                    "fabric" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion)
                )
            )
        )
    )

    maven(
        id = "smartbrainlib",
        group = "net.tslat.smartbrainlib",
        artifact = "SmartBrainLib",
        mcVersionToVersion = mapOf(
            "1.20.1" to "1.15",
            "1.21.1" to "1.16.10"
        ),
        mapping = listOf(
            LoaderMapping(
                "1.20.1", mapOf(
                    "forge" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion),
                    "fabric" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion)
                )
            ),
            LoaderMapping(
                "1.21.1", mapOf(
                    "neoforge" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion),
                    "fabric" to LoaderConfig(ArtifactTransformers.artifactDashLoaderDashMcVersion)
                )
            )
        )
    )
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "CalypsosMobs"