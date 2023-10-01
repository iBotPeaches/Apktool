import proguard.gradle.ProGuardTask

val apktoolVersion: String by rootProject.extra

plugins {
    alias(libs.plugins.shadow)
    application
}

// Buildscript is deprecated, but the alternative approach does not support expanded properties
// https://github.com/gradle/gradle/issues/9830
// So we must hard-code the version here.
buildscript {
    dependencies {
        // Proguard doesn't support plugin DSL - https://github.com/Guardsquare/proguard/issues/225
        classpath(libs.proguard)
    }
}

dependencies {
    implementation(libs.commons.cli)
    implementation(project(":brut.apktool:apktool-lib"))
}

application {
    mainClass.set("brut.apktool.Main")

    tasks.run.get().workingDir = file(System.getProperty("user.dir"))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "brut.apktool.Main"
    }
}

tasks.register<Delete>("cleanOutputDirectory") {
    delete(fileTree("build/libs") {
        exclude("apktool-cli-all.jar")
    })
}

tasks.register<ProGuardTask>("proguard") {
    dependsOn("cleanOutputDirectory")
    dependsOn("shadowJar")
    injars(tasks.named("shadowJar").get().outputs.files)

    val javaHome = System.getProperty("java.home")
    if (JavaVersion.current() <= JavaVersion.VERSION_1_8) {
        libraryjars("$javaHome/lib/jce.jar")
        libraryjars("$javaHome/lib/rt.jar")
    } else {
        libraryjars(mapOf("jarfilter" to "!**.jar", "filter" to "!module-info.class"),
            {
                "$javaHome/jmods/"
            }
        )
    }

    dontobfuscate()
    dontoptimize()

    keep("class brut.apktool.Main { public static void main(java.lang.String[]); }")
    keepclassmembers("enum * { public static **[] values(); public static ** valueOf(java.lang.String); }")
    dontwarn("com.google.common.base.**")
    dontwarn("com.google.common.collect.**")
    dontwarn("com.google.common.util.**")
    dontwarn("javax.xml.xpath.**")
    dontnote("**")

    val outPath = "build/libs/apktool-$apktoolVersion.jar"
    outjars(outPath)
}
