import java.io.ByteArrayOutputStream
import dev.ithundxr.silk.ChangelogText
import me.modmuss50.mpp.ReleaseType

plugins {
    java
    `maven-publish`
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "0.3.4" // https://github.com/modmuss50/mod-publish-plugin
    id("dev.ithundxr.silk") version "0.11.15" // https://github.com/IThundxr/silk
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

println("Phonos v${"mod_version"()}")

val isRelease = System.getenv("RELEASE_BUILD")?.toBoolean() ?: false
val buildNumber = System.getenv("GITHUB_RUN_NUBMER")?.toInt()
val gitHash = "\"${calculateGitHash() + (if (hasUnstaged()) "-modified" else "")}\""

base.archivesName.set("archives_base_name"())
group = "maven_group"()

// Formats the mod version to include the loader, Minecraft version and build number (if present)
// example: 1.0.0-beta.0+fabric-1.20.1-build.100 (or -local)
val build = buildNumber?. let { "-build.${it}" } ?: "-local"

var gitBranchLabel = "";
if ("mod_version"().endsWith("-alpha")) {
    gitBranchLabel = "-" + calculateGitBranch().replace("/", "_")
}

version = "${"mod_version"()}${gitBranchLabel}+fabric-${"minecraft_version"() + if (isRelease) "" else build}"

repositories {
    exclusiveMaven("https://api.modrinth.com/maven", "maven.modrinth")
    exclusiveMaven("https://maven.terraformersmc.com", "com.terraformersmc") // Mod Menu
    exclusiveMaven("https://maven.maxhenkel.de/releases", "de.maxhenkel") // Simple Voice Chat
    exclusiveMaven( // YACL
        "https://maven.isxander.dev/releases",
        "dev.isxander",
        "org.quiltmc.parsers"
    )
    exclusiveMaven("https://repo.sleeping.town", "com.unascribed") // Lib39
    exclusiveMaven("https://jitpack.io", "com.github.koca2000") // NBS4j
}

dependencies {
    minecraft("com.mojang:minecraft:${"minecraft_version"()}")
    mappings("net.fabricmc:yarn:${"yarn_mappings"()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${"loader_version"()}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${"fabric_version"()}")

    modImplementation("maven.modrinth:jsonem:${"jsonem_version"()}")
    include("maven.modrinth:jsonem:${"jsonem_version"()}")

    implementation("com.github.koca2000:NBS4j:1.0")
    include("com.github.koca2000:NBS4j:1.0")

    modApi("com.terraformersmc:modmenu:${"modmenu_version"()}") { isTransitive = false }

    modImplementation("dev.isxander:yet-another-config-lib:${"yacl_version"()}")

    //modCompileOnly("de.maxhenkel.voicechat:voicechat-api:${"voicechat_api_version"()}")
    modCompileOnly("maven.modrinth:simple-voice-chat:fabric-${"voicechat_version"()}") // we do creative mixins, so can't just use the API

    if ("enable_simple_voice_chat"().toBoolean()) {
        modLocalRuntime("maven.modrinth:simple-voice-chat:fabric-${"voicechat_version"()}")
    }

    if ("enable_music_disc_mods"().toBoolean()) {
        modLocalRuntime("maven.modrinth:more-music-discs:${"more_music_discs_version"()}") { isTransitive = false }
        modLocalRuntime("maven.modrinth:spindlemark:${"spindlemark_version"()}") { isTransitive = false }
        modLocalRuntime("com.unascribed:lib39-core:${"lib39_version"()}")
        modLocalRuntime("com.unascribed:lib39-keygen:${"lib39_version"()}")
    }
}

tasks.processResources {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand(mapOf(
            "version" to version,
            "fabric_loader_version" to "loader_version"(),
            "fabric_api_version" to "fabric_version"(),
            "yacl_version" to "yacl_version"(),
        ))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    from("LICENSE")

    manifest {
        attributes(mapOf("Git-Hash" to gitHash))
    }
}

tasks.named<Jar>("sourcesJar") {
    from("LICENSE")

    manifest {
        attributes(mapOf("Git-Hash" to gitHash))
    }
}

loom {
    accessWidenerPath = file("src/main/resources/phonos.accesswidener")

    runs.configureEach {
        vmArg("-XX:+AllowEnhancedClassRedefinition")
        vmArg("-XX:+IgnoreUnrecognizedVMOptions")
        vmArg("-Dmixin.debug.export=true")
        vmArg("-Dfabric-tag-conventions-v2.missingTagTranslationWarning=VERBOSE")
    }
}

fabricApi {
    configureDataGeneration {
        client = true
        outputDirectory = file("src/generated/resources")
    }
}


fun calculateGitHash(): String {
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "HEAD")
            standardOutput = stdout
        }
        return stdout.toString().trim()
    } catch(ignored: Throwable) {
        return "unknown"
    }
}

fun calculateGitBranch(): String {
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            standardOutput = stdout
        }
        return stdout.toString().trim()
    } catch(ignored: Throwable) {
        return "unknown"
    }
}

fun hasUnstaged(): Boolean {
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "status", "--porcelain")
            standardOutput = stdout
        }
        val result = stdout.toString().replace(Regex("M gradlew(\\.bat)?"), "").trimEnd()
        if (result.isNotEmpty())
            println("Found stageable results:\n${result}\n")
        return result.isNotEmpty()
    }  catch(ignored: Throwable) {
        return false
    }
}

fun RepositoryHandler.exclusiveMaven(url: String, vararg groups: String) {
    exclusiveContent {
        forRepository { maven(url) }
        filter {
            groups.forEach {
                includeGroup(it)
            }
        }
    }
}

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
        ?: throw IllegalStateException("Property $this is not defined")
}

publishMods {
    file = tasks.remapJar.get().archiveFile
    version.set(project.version.toString())
    changelog = ChangelogText.getChangelogText(rootProject).toString()
    type = ReleaseType.valueOf(System.getenv().getOrDefault("RELEASE_TYPE", "STABLE"))
    displayName = "Phonos v${"mod_version"()} Fabric ${"minecraft_version"()}"
    modLoaders.add("fabric")

    modrinth {
        projectId = "modrinth_id"()
        accessToken = System.getenv("MODRINTH_TOKEN")
        minecraftVersions.add("minecraft_version"())
    }
}

tasks.register("SemaphorePublish") {
    dependsOn(":build", ":publishMods")
}

tasks.register("SemaphoreChangelog") {
    doLast {
        val changelog = ChangelogText.getChangelogText(rootProject)
        val changelogFile = file("build/changelog.md")
        changelogFile.parentFile.mkdirs()
        changelogFile.writeText(changelog.toString())
    }
}