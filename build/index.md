---
layout: other
title: Apktool - Build Guide
description: Apktool - Build Guide
---

# How to Build Apktool from source
Apktool is a collection of 1 project, containing 4 sub-projects and a few dependencies.

  * **brut.apktool.lib** - (Main, all the Library code)
  * **brut.apktool.cli** - The cli interface of the program
  * **brut.j.dir** - Utility project
  * **brut.j.util** - Utility project
  * **brut.j.common** - Utility project

The main project can be found below

[https://github.com/iBotPeaches/Apktool](https://github.com/iBotPeaches/Apktool)

### Requirements
  * JDK (7 or 8). No OpenJDK
  * git

### Build Steps
We use gradle to build. It's pretty easy. First clone the repository.

  1. `git clone git://github.com/iBotPeaches/Apktool.git`
  2. `cd Apktool`
  3. For steps 3-5 use `./gradlew` for unix based systems or `gradlew.bat` for windows.
  4. `[./gradlew][gradlew.bat] build fatJar` - Builds Apktool, including final binary.
  5. Optional (You may build a Proguard jar) `[./gradlew][gradlew.bat] build fatJar proguard`
  
After 1-2 minutes you should have a jar file at

`./brut.apktool/apktool-cli/build/libs/apktool-xxxxx.jar`
