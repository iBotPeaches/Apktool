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

val proguardGradleVersion: String by rootProject.extra
val commonsCliVersion: String by rootProject.extra

plugins {
    id("com.github.johnrengelman.shadow")
    application
}

dependencies {
    compileOnly("com.guardsquare:proguard-gradle:$proguardGradleVersion")
}

configurations {
    compileClasspath {
        exclude(group = "com.android.tools.build")
    }
}

dependencies {
    implementation("commons-cli:commons-cli:$commonsCliVersion")
    implementation(project(":brut.apktool:apktool-lib"))
}

application {
    mainClass.set("brut.apktool.Main")

    tasks.run.get().workingDir = file(System.getProperty("user.dir"))
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "brut.apktool.Main"
    }
}
