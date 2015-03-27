---
layout: other
description: Apktool - How to Install
title: Apktool - How to Install
---

# Install Instructions

## Quick Check
  * Apktool 2.x (Versions after `1.5.2`)
    1. Is Java 1.7 installed?
    1. Does executing <kbd>java -version</kbd> on command line / command prompt return 1.7?
    1. If not, please install Java 7 and make it the default.
  * Apktool 1.x (Versions prior to `1.5.2`)
    1. Is Java 1.6 or higher installed?
    1. Does executing <kbd>java -version</kbd> on command line / command prompt return 1.6 or above?
    1. If not, please install Java 6 or Java 7.

## Installation for Apktool 2.x
  * **Windows**:
    1. Download Windows [wrapper script](https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/windows/apktool.bat) (Right click, Save Link As `apktool.bat`)
    1. Download apktool-2 ([find newest here](https://bitbucket.org/iBotPeaches/apktool/downloads))
    1. Rename downloaded jar to `apktool.jar`
    1. Move both files (`apktool.jar` & `apktool.bat`) to your Windows directory (Usually `C://Windows`)
    1. If you do not have access to `C://Windows`, you may place the two files anywhere then add that directory to your Environment Variables System PATH variable.
    1. Try running <kbd>apktool</kbd> via command prompt

  * **Linux**:
    1. Download Linux [wrapper script](https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/linux/apktool) (Right click, Save Link As `apktool`)
    1. Download apktool-2 ([find newest here](https://bitbucket.org/iBotPeaches/apktool/downloads))
    1. Make sure you have the 32bit libraries (`ia32-libs`) downloaded and installed by your linux package manager, if you are on a 64bit unix system.
    1. (This helps provide support for the 32bit native binary aapt, which is required by apktool)
    1. Rename downloaded jar to `apktool.jar`
    1. Move both files (`apktool.jar` & `apktool`) to `/usr/local/bin` (root needed)
    1. Make sure both files are executable (`chmod +x`)
    1. Try running <kbd>apktool</kbd> via cli

  * **Mac OS X**:
    1. Download Mac [wrapper script](https://raw.githubusercontent.com/iBotPeaches/Apktool/master/scripts/osx/apktool) (Right click, Save Link As `apktool`)
    1. Download apktool-2 ([find newest here](https://bitbucket.org/iBotPeaches/apktool/downloads))
    1. Rename downloaded jar to `apktool.jar`
    1. Move both files (`apktool.jar` & `apktool`) to `/usr/local/bin` (root needed)
    1. Make sure both files are executable (`chmod +x`)
    1. Try running <kbd>apktool</kbd> via cli

**Note** - Wrapper scripts are not needed, but helpful so you don't have to type <kbd>java -jar apktool.jar</kbd> over and over.

## Installation for Apktool 1.x

  * Windows:
    1. Download `apktool-install-windows-*` file
    1. Download `apktool-*` file
    1. Unpack both to your Windows directory
  * Linux:
    1. Download `apktool-install-linux-*` file
    1. Download `apktool-*` file
    1. Unpack both to `/usr/local/bin` directory (you must have root permissions)
  * Mac OS X:
    1. Download `apktool-install-macos-*` file
    1. Download `apktool-*` file
    1. Unpack both to `/usr/local/bin` directory (you must have root permissions)