val apktoolVersion: String by rootProject.extra

plugins {
    application
}

val r8: Configuration by configurations.creating

dependencies {
    implementation(libs.commons.cli)
    implementation(project(":brut.apktool:apktool-lib"))
    r8(libs.r8)
}

application {
    mainClass.set("brut.apktool.Main")

    tasks.run.get().workingDir = file(System.getProperty("user.dir"))
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.register<Delete>("cleanOutputDirectory") {
    delete(fileTree("build/libs") {
        exclude("apktool-cli-all.jar")
    })
}

val shadowJar = tasks.create("shadowJar", Jar::class) {
    dependsOn("build")
    dependsOn("cleanOutputDirectory")

    group = "build"
    description = "Creates a single executable JAR with all dependencies"
    manifest.attributes["Main-Class"] = "brut.apktool.Main"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)

    from(dependencies)
    with(tasks.jar.get())
}

tasks.register<JavaExec>("proguard") {
    dependsOn("shadowJar")

    onlyIf {
        JavaVersion.current().isJava11Compatible
    }

    val proguardRules = file("proguard-rules.pro")
    val originalJar = shadowJar.outputs.files.singleFile

    inputs.files(originalJar.toString(), proguardRules)
    outputs.file("build/libs/apktool-$apktoolVersion.jar")

    classpath(r8)
    mainClass.set("com.android.tools.r8.R8")

    args = mutableListOf(
        "--release",
        "--classfile",
        "--no-minification",
        "--map-diagnostics:UnusedProguardKeepRuleDiagnostic", "info", "none",
        "--lib", javaLauncher.get().metadata.installationPath.toString(),
        "--output", outputs.files.singleFile.toString(),
        "--pg-conf", proguardRules.toString(),
        originalJar.toString()
    )
}

tasks.getByPath(":release").dependsOn("proguard")
