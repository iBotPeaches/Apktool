dependencies {
    api(project(":brut.j.common"))
    api(project(":brut.j.util"))
    api(project(":brut.j.xml"))
    api(project(":brut.j.yaml"))
    api(project(":brut.j.zip"))

    implementation(libs.baksmali)
    implementation(libs.smali)
    implementation(libs.guava)

    testImplementation(libs.junit)
    testImplementation(libs.xmlunit)
}

tasks {
    processResources {
        from("src/main/resources") {
            include("**/*.jar")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        includeEmptyDirs = false
    }

    test {
        // https://github.com/iBotPeaches/Apktool/issues/3174 - CVE-2023-22036
        // Increases validation of extra field of zip header. Some older Android apps
        // used this field to store data violating the zip specification.
        systemProperty("jdk.util.zip.disableZip64ExtraFieldValidation", true)

        // Configure the JVM to run in headless mode for AWT/X11 graphical operations.
        // Required for ImageIO operations, such as 9-patch image processing.
        systemProperty("java.awt.headless", true)
    }
}
