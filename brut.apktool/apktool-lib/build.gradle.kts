val baksmaliVersion: String by rootProject.extra
val smaliVersion: String by rootProject.extra
val xmlpullVersion: String by rootProject.extra
val guavaVersion: String by rootProject.extra
val commonsLangVersion: String by rootProject.extra
val commonsIoVersion: String by rootProject.extra
val commonsTextVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val xmlunitVersion: String by rootProject.extra

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

    implementation("com.android.tools.smali:smali-baksmali:$baksmaliVersion")
    implementation("com.android.tools.smali:smali:$smaliVersion")
    implementation("xpp3:xpp3:$xmlpullVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.apache.commons:commons-text:$commonsTextVersion")

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.xmlunit:xmlunit-legacy:$xmlunitVersion")
}
