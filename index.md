---
layout: home
tags: apktool, android, apk, reengineering, smali, decode, resources, xml, resources.arsc, AndroidManifest, classes.dex
description: Apktool - A tool for reverse engineering 3rd party, closed, binary Android apps. It can decode resources to nearly original form and rebuild them after making some modifications
---
#Apktool [![Build Status](https://travis-ci.org/iBotPeaches/Apktool.svg?branch=master)](https://travis-ci.org/iBotPeaches/Apktool) [![Software License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](https://github.com/iBotPeaches/Apktool/blob/master/LICENSE)

A tool for reverse engineering 3rd party, closed, binary Android apps. It can decode resources to nearly original form and rebuild them after making some modifications; it makes possible to debug smali code step by step. Also it makes working with an app easier because of project-like file structure and automation of some repetitive tasks like building apk, etc.

It is **NOT** intended for piracy and other non-legal uses. It could be used for localizing, adding some features or support for custom platforms, analyzing applications and much more.

## Features
 * Disassembling resources to nearly original form (including `resources.arsc`, `classes.dex`, `9.png.` and `XMLs`)
 * Rebuilding decoded resources back to binary APK/JAR
 * Organizing and handling APKs that depend on framework resources
 * Smali Debugging (to be removed in `2.1.0` in favor of [IdeaSmali](https://github.com/JesusFreke/smali/wiki/smalidea))
 * Helping with repetitive tasks

## Requirements
 * Java 7 (JRE 1.7)
 * Basic knowledge of Android SDK, AAPT and smali

## Install Instructions
 * Read [Install Docs]({{ site.github.url }}/install)
 
## Links of Interest
 * [XDA Thread](http://forum.xda-developers.com/showthread.php?t=1755243) - For those who wish to communicate on XDA-Developers for community support
 * [Smali Project](https://github.com/JesusFreke/smali) - Smali Project is the tool used in the disassembling of `.dex` files
 * [Freenode #apktool](http://webchat.freenode.net/?channels=apktool) - IRC Channel for support, bugs and discussions

## Authors
<ul>
  {% for author in site.data.authors %}
    <li><a href="https://github.com/{{ author.github }}">{{ author.name }}</a> - {{ author.note }}</li>
  {% endfor %}
</ul>

## News
<ul>
  {% for release in site.data.releases %}
   <li><strong>{{ release.date | date_to_string }}</strong> - {{ release.title }} {{ release.text | markdownify }} </li>
  {% endfor %}
</ul>

## License
Apktool is licensed under the Apache 2.0 License - see the [LICENSE](https://github.com/iBotPeaches/Apktool/blob/master/LICENSE) file for more details