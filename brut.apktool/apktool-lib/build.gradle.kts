val gitRevision: String by rootProject.extra
val apktoolVersion: String by rootProject.extra

tasks {
    processResources {
        from("src/main/resources/properties") {
            include("**/*.properties")
            into("properties")
            expand("version" to apktoolVersion, "gitrev" to gitRevision)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        from("src/main/resources") {
            include("**/*.jar")
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        includeEmptyDirs = false
    }

    test {
        // https://github.com/iBotPeaches/Apktool/issues/3174 - CVE-2023-22036
        // Increases validation of extra field of zip header. Some older Android applications
        // used this field to store data violating the zip specification.
        systemProperty("jdk.util.zip.disableZip64ExtraFieldValidation", true)
    }
}

dependencies {
    api(project(":brut.j.dir"))
    api(project(":brut.j.util"))
    api(project(":brut.j.common"))

    implementation(libs.baksmali)
    implementation(libs.smali)
    implementation(libs.xmlpull)
    implementation(libs.guava)
    implementation(libs.commons.lang3)
    implementation(libs.commons.io)
    implementation(libs.commons.text)

    testImplementation(libs.junit)
    testImplementation(libs.xmlunit)
}
