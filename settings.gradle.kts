rootProject.name = "apktool-cli"
include("brut.j.common", "brut.j.util", "brut.j.dir", "brut.apktool:apktool-lib", "brut.apktool:apktool-cli")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {}
    }
}
