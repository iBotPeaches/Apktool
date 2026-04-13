dependencies {
    implementation(project(":brut.apktool:apktool-lib"))
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.annprocess)
}

sourceSets {
    main {
        resources {
            // Include testapp resources from apktool-lib for benchmark fixtures.
            srcDir("${project(":brut.apktool:apktool-lib").projectDir}/src/test/resources")
        }
    }
}

tasks.register("benchmarkJar", Jar::class) {
    dependsOn("compileJava", "processResources")

    group = "build"
    description = "Creates a fat JAR for running JMH benchmarks"
    archiveFileName.set("apktool-benchmarks.jar")

    manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Use lazy provider so Gradle defers classpath resolution to execution time and
    // automatically schedules :apktool-lib:jar (and other dependency JARs) before this task.
    from(configurations.runtimeClasspath.map { classpath -> classpath.map(::zipTree) })
    with(tasks.jar.get())
}
