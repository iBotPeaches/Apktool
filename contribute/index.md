---
layout: other
title: Apktool Contribute
---

##Contributing to Apktool

### Report a bug
Apktool gets plenty of issue reports not related to apktool at all. People use this tool, but they don't know how how to edit XML files, sign apks or even install them.
I find it funny that some people go the hard route of reverse engineering when they don't even know how to work with application sources.

So... if you get some errors from apktool when decoding or building apk, then this is probably a bug in apktool and you should report it.
But if you have succeeded at decoding and building, but the app doesn't work on your device, then I suggest you do some tests:

 * don't do anything with original apk file, just install it using the same way as for apk built by apktool. If it fails, then it can't be related to apktool, because you didn't even use it. Typical scenario: installing framework-res.apk by "adb push" command, which will certainly give you a lot of FCs.
 * unzip apk, remove META-INF dir, zip it back, sign and install. This will remove original signatures, but rest of the app will be intact. If you will fail at this point, you could be sure, that your problems are related to signing, not apktool.
 * rebuild apk without doing any changes to it (<kbd>apktool d</kbd> & <kbd>apktool b</kbd>). If apktool succeeds, but apk won't work, then it's probably an apktool bug and you should report it.
 * if an app was working earlier, but after doing some changes - it doesn't, this may be a bug of apktool, but more likely it's your fault.
 
After all of this, if you are ready to report a bug. Please make sure to answer the following questions in your bug report.

 * What steps will reproduce this problem?
 * What is the expected output? What do you see instead?
 * What version of the tool are you using? On what Operating System?
 * Any additional information? (ROM, AOSP version)
 
[Report a bug here](https://github.com/iBotPeaches/Apktool/issues/new)

### I want to work on the project
Great! Explore and fix some bugs while you are here. You may find the project [here](https://github.com/iBotPeaches/Apktool) and [contributing hints](https://github.com/iBotPeaches/Apktool/blob/master/CONTRIBUTING.md).

### This documentation could use some work
Good thing it is open source! Clean up the documentation [here](https://github.com/iBotPeaches/Apktool/tree/gh-pages).