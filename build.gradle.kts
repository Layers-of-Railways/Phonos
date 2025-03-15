import java.io.ByteArrayOutputStream

plugins {
    java
    `maven-publish`
    id("fabric-loom") version "1.7-SNAPSHOT"
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
    maven("https://api.modrinth.com/maven")
    maven("https://maven.terraformersmc.com")
    maven("https://maven.maxhenkel.de/repository/public") // Simple Voice Chat
    maven("https://mvn.devos.one/snapshots/") // Create Fabric, Porting Lib, Forge Tags, Milk Lib, Registrate Fabric
    maven("https://maven.jamieswhiteshirt.com/libs-release") // Reach Entity Attributes
    exclusiveMaven( // Flywheel, Registrate
        "https://maven.tterrag.com/",
        "com.tterrag.registrate",
        "com.jozufozu.flywheel"
    )
    maven("https://maven.isxander.dev/releases") // YACL
    exclusiveMaven("https://repo.sleeping.town", "com.unascribed")
}

dependencies {
    minecraft("com.mojang:minecraft:${"minecraft_version"()}")
    mappings("net.fabricmc:yarn:${"yarn_mappings"()}:v2")
    modImplementation("net.fabricmc:fabric-loader:${"loader_version"()}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${"fabric_version"()}")

    modImplementation("maven.modrinth:jsonem:${"jsonem_version"()}")
    include("maven.modrinth:jsonem:${"jsonem_version"()}")

    modApi("com.terraformersmc:modmenu:${"modmenu_version"()}") { isTransitive = false }

    modImplementation("dev.isxander:yet-another-config-lib:${"yacl_version"()}")

    //modCompileOnly("de.maxhenkel.voicechat:voicechat-api:${"voicechat_api_version"()}")
    modCompileOnly("maven.modrinth:simple-voice-chat:fabric-${"voicechat_version"()}")

    if ("enable_simple_voice_chat"().toBoolean()) {
        modLocalRuntime("maven.modrinth:simple-voice-chat:fabric-${"voicechat_version"()}")
    }

    if ("enable_create"().toBoolean()) {
        // Create - dependencies are added transitively
        modLocalRuntime("com.simibubi.create:create-fabric-${"minecraft_version"()}:${"create_fabric_version"()}")
    }

    if ("enable_music_disc_mods"().toBoolean()) {
        modLocalRuntime("maven.modrinth:more-music-discs:${"more_music_discs_version"()}") { isTransitive = false }
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