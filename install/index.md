---
layout: other
description: Apktool - How to Install
title: Apktool - How to Install
---

# Install Instructions

## Quick Check
  1. Is at least Java 1.8 installed?
  1. Does executing <kbd>java -version</kbd> on command line / command prompt return 1.8 or greater?
  1. If not, please install Java 8+ and make it the default. (Java 7 will also work at this time)

## Installation for Apktool
  * **Windows**:
    1. Download Windows [wrapper script](https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/windows/apktool.bat) (Right click, Save Link As `apktool.bat`)
    1. Download apktool-2 ([find newest here](https://bitbucket.org/iBotPeaches/apktool/downloads/))
    1. Rename downloaded jar to `apktool.jar`
    1. Move both files (`apktool.jar` & `apktool.bat`) to your Windows directory (Usually `C://Windows`)
    1. If you do not have access to `C://Windows`, you may place the two files anywhere then add that directory to your Environment Variables System PATH variable.
    1. Try running <kbd>apktool</kbd> via command prompt

  * **Linux**:
    1. Download Linux [wrapper script](https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool) (Right click, Save Link As `apktool`)
    1. Download apktool-2 ([find newest here](https://bitbucket.org/iBotPeaches/apktool/downloads/))
    1. Rename downloaded jar to `apktool.jar`
    1. Move both files (`apktool.jar` & `apktool`) to `/usr/local/bin` (root needed)
    1. Make sure both files are executable (`chmod +x`)
    1. Try running <kbd>apktool</kbd> via cli

  * **macOS**:
    1. Download Mac [wrapper script](https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/osx/apktool) (Right click, Save Link As `apktool`)
    1. Download apktool-2 ([find newest here](https://bitbucket.org/iBotPeaches/apktool/downloads/))
    1. Rename downloaded jar to `apktool.jar`
    1. Move both files (`apktool.jar` & `apktool`) to `/usr/local/bin` (root needed)
    1. Make sure both files are executable (`chmod +x`)
    1. Try running <kbd>apktool</kbd> via cli
 
    Or you can install apktool via _Homebrew_:
    1. Install Homebrew as described [in this page](https://brew.sh/)
    1. Execute command `brew install apktool` in terminal (no root needed).
 The latest version will be installed in `/usr/local/Cellar/apktool/[version]/` and linked to `/usr/local/bin/apktool`.
    1. Try running <kbd>apktool</kbd> via cli

**Note** - Wrapper scripts are not needed, but helpful so you don't have to type <kbd>java -jar apktool.jar</kbd> over and over.
