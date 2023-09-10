val commonsIoVersion: String by rootProject.extra
val guavaVersion: String by rootProject.extra

dependencies {
    implementation(project(":brut.j.common"))
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("com.google.guava:guava:$guavaVersion")
}
