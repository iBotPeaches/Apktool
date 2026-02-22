import java.io.ByteArrayOutputStream

val version = "3.0.2"
val suffix = "SNAPSHOT"

// Strings embedded into the build.
var gitRevision by extra("")
var apktoolVersion by extra("")

defaultTasks("build", "shadowJar", "proguard")

// Functions
val gitDescribe: String? by lazy {
    try {
        val result = providers.exec {
            commandLine("git", "describe", "--tags")
        }
        result.standardOutput.asText.get().trim().replace("-g", "-")
    } catch (e: Exception) {
        null
    }
}

val gitBranch: String? by lazy {
    try {
        val result = providers.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
        }
        result.standardOutput.asText.get().trim()
    } catch (e: Exception) {
        null
    }
}

if ("release" !in gradle.startParameter.taskNames) {
    val hash = gitDescribe

    if (hash == null) {
        gitRevision = "dirty"
        apktoolVersion = "$version-dirty"
        project.logger.lifecycle("Building SNAPSHOT (no .git folder found)")
    } else {
        gitRevision = hash
        apktoolVersion = "$hash-SNAPSHOT"
        project.logger.lifecycle("Building SNAPSHOT ($gitBranch): $gitRevision")
    }
} else {
    gitRevision = ""
    apktoolVersion = if (suffix.isNotEmpty()) "$version-$suffix" else version;
    project.logger.lifecycle("Building RELEASE ($gitBranch): $apktoolVersion")
}

plugins {
    `java-library`
    if (JavaVersion.current().isJava11Compatible) {
        alias(libs.plugins.vanniktech.maven.publish) apply false
    }
}

allprojects {
    repositories {
        mavenCentral()
        // Obtain baksmali/smali from source builds - https://github.com/iBotPeaches/smali
        // Remove when official smali releases come out again.
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.iBotPeaches.smali")
            }
        }
        google()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    val mavenProjects = arrayOf(
        "brut.j.common", "brut.j.util", "brut.j.dir", "brut.j.xml", "brut.j.yaml",
        "apktool-lib", "apktool-cli"
    )

    if (project.name in mavenProjects && JavaVersion.current().isJava11Compatible) {
        apply(from = "${rootProject.projectDir}/gradle/scripts/publishing.gradle")
    }
}

tasks.register("release") {
    // Used for official releases.
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:-options")
    options.compilerArgs.add("--release 8")

    options.encoding = "UTF-8"
}
