import java.io.ByteArrayOutputStream

val baksmaliVersion by extra("3.0.3")
val commonsCliVersion by extra("1.5.0")
val commonsIoVersion by extra("2.13.0")
val commonsLangVersion by extra("3.13.0")
val commonsTextVersion by extra("1.10.0")
val guavaVersion by extra("32.0.1-jre")
val junitVersion by extra("4.13.2")
val smaliVersion by extra("3.0.3")
val xmlpullVersion by extra("1.1.4c")
val xmlunitVersion by extra("2.9.1")

val version = "2.8.2"
var mavenVersion = "unspecified"
val suffix = "SNAPSHOT"

// Strings embedded into the build.
var gitRevision by extra("")
var apktoolVersion by extra("")

defaultTasks("build", "shadowJar", "proguard")

plugins {
    `java-library`
}

buildscript {
    repositories {
        gradlePluginPortal()
        google()
    }
    dependencies {
        classpath("gradle.plugin.com.github.johnrengelman:shadow:8.0.0")
        classpath("gradle.plugin.com.hierynomus.gradle.plugins:license-gradle-plugin:0.16.1")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:-options")
    options.compilerArgs.add("--release 8")

    options.encoding = "UTF-8"
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
}

// Used for publishing snapshots to maven.
task("snapshot") {

}

// Used for official releases.
task("release") {

}

// Functions
val gitDescribe: String? by lazy {
    val stdout = ByteArrayOutputStream()
    try {
        rootProject.exec {
            commandLine("git", "describe", "--tags")
            standardOutput = stdout
        }
        stdout.toString().trim().replace("-g", "-")
    } catch (e: Exception) {
        null
    }
}
val gitBranch: String? by lazy {
    val stdout = ByteArrayOutputStream()
    try {
        rootProject.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        null
    }
}

// Runtime
if ("release" !in gradle.startParameter.taskNames) {
    val hash = this.gitDescribe

    if (hash == null) {
        gitRevision = "dirty"
        apktoolVersion = "$version-dirty"
        project.logger.lifecycle("Building SNAPSHOT (no .git folder found)")
    } else {
        gitRevision = hash
        apktoolVersion = "$hash-SNAPSHOT"
        mavenVersion = "$version-SNAPSHOT"
        project.logger.lifecycle("Building SNAPSHOT (${gitBranch}): $gitRevision")
    }
} else {
    gitRevision = ""
    apktoolVersion = if (suffix.isNotEmpty()) "$version-$suffix" else version;
    mavenVersion = version
    project.logger.lifecycle("Building RELEASE (${gitBranch}): ${apktoolVersion}}")
}

