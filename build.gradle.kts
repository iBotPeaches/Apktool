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
import java.nio.charset.StandardCharsets

val baksmaliVersion by extra("3.0.3")
val commonsCliVersion by extra("1.5.0")
val commonsIoVersion by extra("2.13.0")
val commonsLangVersion by extra("3.13.0")
val commonsTextVersion by extra("1.10.0")
val guavaVersion by extra("32.0.1-jre")
val junitVersion by extra("4.13.2")
val proguardGradleVersion by extra("7.3.2")
val smaliVersion by extra("3.0.3")
val xmlpullVersion by extra("1.1.4c")
val xmlunitVersion by extra("2.9.1")

buildscript {
    repositories {
        gradlePluginPortal()
        google()
    }
    dependencies {
        classpath("gradle.plugin.com.github.johnrengelman:shadow:8.0.0")
        classpath("gradle.plugin.com.hierynomus.gradle.plugins:license-gradle-plugin:0.16.1")
    }
}

plugins {
    `java-library`
}

val version = "2.8.2"
val suffix = "SNAPSHOT"

defaultTasks("build", "shadowJar", "proguard")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "java")
}
