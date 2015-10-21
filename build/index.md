---
layout: other
title: Apktool - Build Guide
description: Apktool - Build Guide
---

# How to Build Apktool from source
Apktool is a collection of 1 project, containing 5 sub-projects.

  * **brut.apktool.lib** - (Main, all the Library code)
  * **brut.apktool.cli** - The cli interface of the program
  * **brut.j.dir** - Utility project
  * **brut.j.util** - Utility project
  * **brut.j.common** - Utility project
  * **smali** - submodule of [JesusFreke's smali](https://github.com/JesusFreke/smali) tool


The main project can be found below

[https://github.com/iBotPeaches/Apktool](https://github.com/iBotPeaches/Apktool)

### Requirements
  * JDK (1.7)
  * git

### Build Steps
We use gradle to build. It's pretty easy. First clone the repository.

  1. `git clone --recursive git://github.com/iBotPeaches/Apktool.git`
  2. `cd Apktool`
  3. `./gradlew` for unix based systems. `gradlew.bat` for windows.
  4. `[./gradlew][gradlew.bat] applyPatches` - Applies `smali` patches, creating `brut.apktool.smali` directory.
  5. `[./gradlew][gradlew.bat] build fatJar` - Builds Apktool, including final binary.
  6. Optional (You may build a Proguard jar) `[./gradlew][gradlew.bat] build fatJar proguard`
  
After 1-2 minutes you should have a jar file at

`./brut.apktool/apktool-cli/build/libs/apktool-xxxxx.jar`


### Cleaning
Cleaning the project, removes all build directories and wipes the `brut.apktool.smali` directory. You must reapply patches via step 4 above to re-create the `brut.apktool.smali`. **This must be done before building Apktool again.**.
