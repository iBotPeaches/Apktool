---
layout: other
title: Apktool - Build Guide
description: Apktool - Build Guide
---

# How to Build Apktool from source
Apktool is a collection of 1 project, containing sub-projects and a few dependencies.

  * **brut.apktool.lib** - (Main, all the Library code)
  * **brut.apktool.cli** - The cli interface of the program
  * **brut.j.dir** - Utility project
  * **brut.j.util** - Utility project
  * **brut.j.common** - Utility project

The main project can be found below

[https://github.com/iBotPeaches/Apktool](https://github.com/iBotPeaches/Apktool)

### Requirements
  * JDK8 (Oracle or OpenJDK)
  * git

### Build Steps
We use gradle to build. First clone the repository using either 
[SSH](ssh://git@github.com:iBotPeaches/Apktool.git) or [HTTPS](https://github.com/iBotPeaches/Apktool.git).

  1. `cd Apktool`
  2. For remaining steps use `./gradlew` for unix based systems or `gradlew.bat` for windows.
  3. `[./gradlew][gradlew.bat] build shadowJar` - Builds Apktool, including final binary.
  4. Optional (You may build a Proguard jar) `[./gradlew][gradlew.bat] build shadowJar proguard`
  
After build completes you should have a jar file at:
`./brut.apktool/apktool-cli/build/libs/apktool-xxxxx.jar`

### Windows Requirements
Windows has some limitations regarding max filepath. At one location in Apktool, we have a 218 character directory path
which means due to the limitation of max 255 characters on Windows we need to enforce some requirements.

This leaves 37 characters total to clone the project on Windows. For example, I can clone this project to the location

    C:/Users/Connor/Desktop/Apktool

This is 31 characters, which allows Apktool to be cloned properly. Cloning the project into a directory longer than 37
characters will not work.
