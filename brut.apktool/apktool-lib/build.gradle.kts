/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

val baksmaliVersion: String by rootProject.extra
val smaliVersion: String by rootProject.extra
val xmlpullVersion: String by rootProject.extra
val guavaVersion: String by rootProject.extra
val commonsLangVersion: String by rootProject.extra
val commonsIoVersion: String by rootProject.extra
val commonsTextVersion: String by rootProject.extra
val junitVersion: String by rootProject.extra
val xmlunitVersion: String by rootProject.extra

plugins {
    `java-library`
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
