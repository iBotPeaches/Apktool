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
import org.apache.tools.ant.filters.*

apply plugin: 'java-library'

processResources {
  from('src/main/resources/properties') {
    include '**/*.properties'
    into 'properties'
    filter(ReplaceTokens, tokens: [version: project.apktool_version, gitrev: project.hash] )
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
  from('src/main/resources/') {
    include '**/*.jar'
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }

  includeEmptyDirs = false
}

dependencies {
    api project(':brut.j.dir')
    api project(':brut.j.util')
    api project(':brut.j.common')

    implementation depends.baksmali
    implementation depends.smali
    implementation depends.xmlpull
    implementation depends.guava
    implementation depends.commons_lang
    implementation depends.commons_io
    implementation depends.commons_text

    testImplementation depends.junit
    testImplementation depends.xmlunit
}
