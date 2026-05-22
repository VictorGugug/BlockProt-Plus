import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GitHub
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.util.Properties

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("org.kohsuke:github-api:1.318")
    }
}

plugins {
    id("org.gradle.java-library")
    id("org.ajoberstar.grgit") version "5.2.1"
}

fun gitBranchName(): String {
    val env = System.getenv()
    if (env["GITHUB_REF"] != null) {
        val branch = env["GITHUB_REF"]!!
        return branch.substring(branch.lastIndexOf("/") + 1)
    }

    return try {
        val branch = grgit.branch.current().name
        branch.substring(branch.lastIndexOf("/") + 1)
    } catch (e: Exception) {
        "master"
    }
}

val env: MutableMap<String, String> = System.getenv()
val blockProtVersion: String by project
val versionSuffix: String by project
val targetJavaVersion: String by project

/**
 * Full version string with auto-incrementing SNAPSHOT counter.
 *
 * Rules:
 *  - If versionSuffix is blank  -> clean release, e.g. "1.3.0"
 *  - If versionSuffix == "SNAPSHOT" -> auto-append counter 1..5, stored in
 *    `.gradle/snapshot-counter.properties` next to this build script.
 *    Counter resets to 1 when blockProtVersion changes.
 *    When counter would exceed 5 the version number is bumped (patch +1,
 *    patch==9 -> minor+1 patch->0, minor==9 -> major+1 minor->0 patch->0)
 *    and the counter resets to 1.
 *
 *    JAR-scan: before advancing, the existing JARs in spigot/build/libs are
 *    scanned to find the highest SNAPSHOT-N already on disk.  If that number
 *    is higher than what the properties file says, we start from there.
 *    This means that if you delete intermediate JARs (e.g. 3 and 4 but keep 5)
 *    the next build will produce SNAPSHOT-6 instead of re-using a lower number.
 *
 *  - Any other suffix -> "$blockProtVersion-$versionSuffix"
 */
fun computeFullVersion(): String {
    val rawSuffix = versionSuffix.trim()
    if (rawSuffix.isBlank()) return blockProtVersion
    if (!rawSuffix.equals("SNAPSHOT", ignoreCase = true)) return "$blockProtVersion-$rawSuffix"

    // ── SNAPSHOT auto-counter ────────────────────────────────────────────────
    val counterFile = file(".gradle/snapshot-counter.properties")
    counterFile.parentFile.mkdirs()

    val props = Properties()
    if (counterFile.exists()) counterFile.inputStream().use<java.io.InputStream, Unit> { props.load(it) }

    val storedVersion = props.getProperty("trackedVersion", "")
    var counter       = props.getProperty("counter", "0").toIntOrNull() ?: 0
    var effectiveVersion = blockProtVersion

    if (storedVersion != blockProtVersion) {
        // Version changed externally -> start fresh (but JAR scan below may still raise the floor)
        counter = 0
        props["trackedVersion"] = blockProtVersion
    }

    // ── Scan existing JARs to find the highest counter already on disk ───────
    // Pattern: BlockProt-{effectiveVersion}-SNAPSHOT-{N}.jar
    // If the user deleted some JARs (e.g. deleted 3 & 4 but kept 5), the counter
    // on disk is still 5 and we continue from there, so no number is ever reused.
    val libsDir = file("spigot/build/libs")
    if (libsDir.exists()) {
        val escapedVersion = effectiveVersion.replace(".", "\\.")
        val snapshotPattern = Regex("""^BlockProt-$escapedVersion-SNAPSHOT-(\d+)\.jar$""")
        val maxOnDisk = libsDir.listFiles()
            ?.mapNotNull { f -> snapshotPattern.find(f.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.maxOrNull() ?: 0
        // Trust whichever is higher: the properties file or what physically exists
        if (maxOnDisk > counter) {
            counter = maxOnDisk
        }
    }

    // Advance to the next build number
    counter++
    if (counter > 5) {
        // Bump version number (patch -> minor -> major cascade)
        val parts = effectiveVersion.split(".").map { it.toInt() }.toMutableList()
        while (parts.size < 3) parts.add(0)
        parts[2]++
        if (parts[2] > 9) { parts[2] = 0; parts[1]++ }
        if (parts[1] > 9) { parts[1] = 0; parts[0]++ }
        effectiveVersion = parts.joinToString(".")
        counter = 1
        // Persist bumped base version so next invocation knows
        props["trackedVersion"] = effectiveVersion
    }

    props["counter"] = counter.toString()
    counterFile.outputStream().use<java.io.OutputStream, Unit> {
        props.store(it, "BlockProt SNAPSHOT counter — do not edit manually")
    }

    return "$effectiveVersion-SNAPSHOT-$counter"
}

val fullVersion: String = computeFullVersion()

allprojects {
    apply(plugin = "org.gradle.java-library")

    group = "de.sean.blockprot"
    version = fullVersion

    repositories {
        mavenLocal()
        maven("https://jitpack.io") {
            name = "JitPack"
        }
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.compileJava {
        options.release.set(21)
    }

    ext["gitBranchName"] = gitBranchName()

    tasks.jar {
        archiveClassifier.set(
            if (ext["gitBranchName"] == "master" || ext["gitBranchName"] == "HEAD" || ext["gitBranchName"] == "main") null
            else (ext["gitBranchName"] as String)
        )
    }
}

tasks.register("github") {
    onlyIf {
        env["GITHUB_TOKEN"] != null
    }

    doLast {
        val github = GitHub.connectUsingOAuth(env["GITHUB_TOKEN"] as String)
        val repository = github.getRepository(env["GITHUB_REPOSITORY"])

        val releaseBuilder = GHReleaseBuilder(repository, version as String)
        releaseBuilder.name(version as String)
        releaseBuilder.body(env["CHANGELOG"] ?: "No changelog.")
        releaseBuilder.commitish(gitBranchName())

        val files = mutableListOf<File?>()
        subprojects.filter { it.name != "common" }.forEach {
            val dir = it.layout.buildDirectory.dir("libs").get().asFile
            files.add(dir.listFiles()?.firstOrNull { file ->
                file.name.startsWith("BlockProt-Reloaded-") && file.name.endsWith(".jar")
                    && !file.name.endsWith("-javadoc.jar")
                    && !file.name.endsWith("-sources.jar")
            })
        }

        val ghRelease = releaseBuilder.create()
        files.forEach {
            ghRelease.uploadAsset(it, "application/java-archive")
        }
        ghRelease.update().name("BlockProt Reloaded $version").update()
    }
}
