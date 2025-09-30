dependencies {
    api(project(":brut.j.common"))
    api(project(":brut.j.util"))
    api(project(":brut.j.dir"))
    api(project(":brut.j.xml"))
    api(project(":brut.j.yaml"))

    implementation(libs.baksmali)
    implementation(libs.smali)
    implementation(libs.guava)
    implementation(libs.commons.lang3)
    implementation(libs.commons.io)
    implementation(libs.commons.text)

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

        // Fix for AWT/X11 graphics environment issues in headless environments
        // Required for tests that use ImageIO operations (nine-patch processing, etc.)
        systemProperty("java.awt.headless", true)
    }
}
