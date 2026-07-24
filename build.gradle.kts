import java.io.ByteArrayOutputStream

val version = "3.1.0"
val suffix = "SNAPSHOT"

// Strings embedded into the build.
var gitRevision by extra("")
var apktoolVersion by extra("")

defaultTasks("build", "shadowJar", "proguard")

require(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    "Building Apktool requires JDK 17 or newer, but Gradle is running on JDK ${JavaVersion.current()}."
}

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
    alias(libs.plugins.vanniktech.maven.publish) apply false
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
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Build with JDK 17, but emit Java 8 compatible bytecode against the Java 8 API.
        options.release.set(8)
    }

    tasks.withType<Test>().configureEach {
        testLogging {
            events("failed", "skipped")
        }
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ descriptor, result ->
            // Only print the summary for the top-level suite (the task itself, not individual classes).
            if (descriptor.parent == null) {
                logger.lifecycle(
                    "[{}] Tests: {} passed, {} failed, {} skipped (total: {})",
                    project.name,
                    result.successfulTestCount,
                    result.failedTestCount,
                    result.skippedTestCount,
                    result.testCount
                )
            }
        }))
    }

    // CI passes -PtestJdkVersion to run the test suite on an older JVM (8/11)
    // while the build itself stays on JDK 17.
    providers.gradleProperty("testJdkVersion").orNull?.toIntOrNull()?.let { testJdkVersion ->
        val toolchains = extensions.getByType<JavaToolchainService>()
        tasks.withType<Test>().configureEach {
            javaLauncher = toolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(testJdkVersion)
                // Zulu ships JDK 8 builds for every OS/arch we test on, including mac arm64.
                vendor = JvmVendorSpec.AZUL
            }
            doFirst {
                val launcher = javaLauncher.get()
                logger.lifecycle(
                    "[{}] Test JVM: {} (runtime: {})",
                    project.name,
                    launcher.executablePath,
                    launcher.metadata.javaRuntimeVersion
                )
            }
        }
    }

    val mavenProjects = arrayOf(
        "brut.j.common", "brut.j.util", "brut.j.dir", "brut.j.xml", "brut.j.yaml",
        "apktool-lib", "apktool-cli"
    )

    if (project.name in mavenProjects) {
        apply(from = "${rootProject.projectDir}/gradle/scripts/publishing.gradle")
    }
}

tasks.register("release") {
    // Used for official releases.
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
