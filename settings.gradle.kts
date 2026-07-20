plugins {
    // Allows Gradle to auto-download the JDK requested via -PtestJdkVersion.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "apktool-cli"
include(
    "brut.j.common", "brut.j.util", "brut.j.dir", "brut.j.xml", "brut.j.yaml",
    "brut.apktool:apktool-lib", "brut.apktool:apktool-cli"
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {}
    }
}
