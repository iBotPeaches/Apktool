val commonsIoVersion: String by rootProject.extra

dependencies {
  implementation(project(":brut.j.common"))
  implementation(project(":brut.j.util"))
  implementation("commons-io:commons-io:$commonsIoVersion")
}
