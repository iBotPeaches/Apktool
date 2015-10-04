---
layout: other
title: Apktool - Changelog
description: Apktool - Changelog / Roadmap
---

## v2.0.2 (in development)
2015.xx.xx

 * Fixed issues with apks that use `.9.xml` files, which improperly triggered 9patch decoder. ([Issue 1005](https://github.com/iBotPeaches/Apktool/issues/1005))
 * Prevent compressing resources that should not be. ([Pull 1020](https://github.com/iBotPeaches/Apktool/pull/1020))
 * `aapt` changes
    * [7bff1857](https://github.com/iBotPeaches/platform_frameworks_base/commit/7bff1857dd104623eaa2e1cd0f56187d3e1317dc) - skip compat functions
    * [7e857f82](https://github.com/iBotPeaches/platform_frameworks_base/commit/7e857f8256dd8c905ca558711711bcc14013955c) - add miui support for `godzillaui`
    * [74edf627](https://github.com/iBotPeaches/platform_frameworks_base/commit/74edf6279cb84c5e1b81f411ce1145ef7ceaaf79) - add miui support for 4 digit `mnc`/`mcc` fields.
 * Updated smali/baksmali to `v2.0.8`
 * Fixed issues with reference attributes being decoded improperly. ([Issue 1023](https://github.com/iBotPeaches/Apktool/issues/1023)) / (Thanks phhusson)
 * Fixed issue with version qualifiers being improperly added during build. ([Issue 928](https://github.com/iBotPeaches/Apktool/issues/928))

## v2.0.1
2015.07.15

 * Fixed version qualifier like `v4` from being ignored during decode. ([Issue 928](https://github.com/iBotPeaches/Apktool/issues/928))
 * Fixed windows helper script from appending current directory into `$PATH`. ([Issue 927](https://github.com/iBotPeaches/Apktool/issues/927)) / (Thanks Tercete)
 * Fixed frameworks that were SharedLibraries from affecting the `apktool.yml` file. ([Issue 936](https://github.com/iBotPeaches/Apktool/issues/936))
 * Fixed apks that were crashing on internal attributes. ([Issue 913](https://github.com/iBotPeaches/Apktool/issues/913))
 * Fixed `ResFileValue(s)` being casted to `ResScalarValues`. ([Issue 921](https://github.com/iBotPeaches/Apktool/issues/921))
 * Fixed reading `ResConfigFlags` twice. ([Issue 924](https://github.com/iBotPeaches/Apktool/issues/924))
 * Expose raw index used for resource lookups. ([Issue 990](https://github.com/iBotPeaches/Apktool/issues/990)) / (Thanks mmastrac)
 * Correctly add `libs` and `lib` folders on `[b]uild`
 
## v2.0.0
2015.04.21

 * [Migration Instructions from 1.5.x to 2.0.x]({{ site.baseurl }}/documentation/#v1-5-x-v2-0-0)
 * **Android 5.1 support**
 * Updated smali/baksmali to `v2.0.5`
 * Updated gradle to `v2.1`
 * Fixed using `-c` to retain original manifest and META-INF folder. ([Issue 118](https://github.com/iBotPeaches/Apktool/issues/118))
 * Fixed handling apks that have unknown files outside of standard aapt allowed resources. ([Issue 174](https://github.com/iBotPeaches/Apktool/issues/174))
 * Fixed aapt incorrectly setting `pkgId`. ([Issue 313](https://github.com/iBotPeaches/Apktool/issues/313)) / (Thanks M1cha)
 * Added new usage output to organize features / parameters.  ([Issue 514](https://github.com/iBotPeaches/Apktool/issues/514))
 * Fixed NPE from malformed 9patch images. ([Issue 470](https://github.com/iBotPeaches/Apktool/issues/470)) / (Thanks Felipe Richards)
 * Fixed aapt requiring `versionName` and `versionCode` via parameter passing. ([Issue 512](https://github.com/iBotPeaches/Apktool/issues/512))
 * Fixed common `aapt` problems by including an internal mac, win and linux aapt. ([Issue 551](https://github.com/iBotPeaches/Apktool/issues/551))
 * Fixed decoding apks that had general access bit thrown. ([Issue 550](https://github.com/iBotPeaches/Apktool/issues/550))
 * Fixed debug mode (`-d`) to fix smali debugging. ([Issue 450](https://github.com/iBotPeaches/Apktool/issues/450)) / (Thanks Ryszard)
 * Adapted smali debugging output to make breakpoint setting easier across IDEs. ([Issue 228](https://github.com/iBotPeaches/Apktool/issues/288)) / (Thanks Ryszard)
 * Fixed characters (`&` & `<`) from being double escaped in `<item>`'s of `arrays.xml`. ([Issue 502](https://github.com/iBotPeaches/Apktool/issues/502))
 * Fixed "multiple substitution" errors with positional and exactly 1 non-positional argument. ([Issue 371](https://github.com/iBotPeaches/Apktool/issues/371))
 * Fixed ignoring `--frame-path` on `[b]`uild. ([Issue 538](https://github.com/iBotPeaches/Apktool/issues/538))
 * Fixed setting `android:debuggable` on debug apks. ([Issue 507](https://github.com/iBotPeaches/Apktool/issues/507))
 * Fixed common "superclass" errors on debug mode. ([Issue 451](https://github.com/iBotPeaches/Apktool/issues/451))
 * Fixed `pkgId` not being set in framework files. ([Issue 569](https://github.com/iBotPeaches/Apktool/issues/569))
 * Added `-m` / `--match-original` feature to allow apks to match original. ([Issue 580](https://github.com/iBotPeaches/Apktool/issues/580))
 * Fixed apks PNGs gaining brightness on rebuild. ([Issue 437](https://github.com/iBotPeaches/Apktool/issues/437))
 * Added dexlib2 (smali2) into Apktool. ([Issue 559](https://github.com/iBotPeaches/Apktool/issues/559))
 * Fixed windows builds caused by `java.nio`. ([Issue 606](https://github.com/iBotPeaches/Apktool/issues/606))
 * Fixed error output being written to `stdout` instead of `stderr`. ([Issue 620](https://github.com/iBotPeaches/Apktool/issues/620))
 * Fixed issue with smali filenames from being too long. ([Issue 537](https://github.com/iBotPeaches/Apktool/issues/537)) / (Thanks JesusFreke)
 * Fixed issue with `INSTALL_FAILED_DEXOPT`. ([Issue 634](https://github.com/iBotPeaches/Apktool/issues/634)) / (Thanks JesusFreke)
 * Fixed issue with apks with multiple packages. ([Issue 583](https://github.com/iBotPeaches/Apktool/issues/583))
 * Fixed issue with decoding `.jar` files. ([Issue 641](https://github.com/iBotPeaches/Apktool/issues/641))
 * Fixed issue with improperly labeling type of `<array>`'s. ([Issue 660](https://github.com/iBotPeaches/Apktool/issues/660))
 * Fixed issue with truncated strings. ([Issue 681](https://github.com/iBotPeaches/Apktool/issues/681)) / (Thanks jtmuhone)
 * Fixed issue with apks with multiple empty types via ignoring them. ([Issue 688](https://github.com/iBotPeaches/Apktool/issues/688))
 * Fixed issue with apks with one package named `android` from decoding. ([Issue 699](https://github.com/iBotPeaches/Apktool/issues/699))
 * Fixed StringBlock by making it thread safe. ([Issue 711](https://github.com/iBotPeaches/Apktool/issues/711)) / (Thanks aluedeke)
 * Fixed truncated `UTF-16` strings. ([Issue 349](https://github.com/iBotPeaches/Apktool/issues/349))
 * Spacing cleanup of 2014. ([Issue 694](https://github.com/iBotPeaches/Apktool/issues/694))
 * Fixed style crash due to malformed styles. ([Issue 307](https://github.com/iBotPeaches/Apktool/issues/307)) 
 * Fixed issue with unknown files being ignored that start with an accepted file name. ([Issue 713](https://github.com/iBotPeaches/Apktool/issues/713))
 * Fixed issue with unknown files being ignored when `-r` was used. ([Issue 716](https://github.com/iBotPeaches/Apktool/issues/716))
 * Fixed issue with renamed manifests such as (`android`, `com.htc` and `miui`). ([Issue 719](https://github.com/iBotPeaches/Apktool/issues/719))
 * Fixed path issues with `UTF8` chars and unknown files. ([Issue 736](https://github.com/iBotPeaches/Apktool/issues/736))
 * Fixed issue with renamed manifest (`com.lge`). ([Issue 740](https://github.com/iBotPeaches/Apktool/issues/740))
 * Fixed incorrect typing of `<array>` items due to incorrect loop index. ([Issue 520](https://github.com/iBotPeaches/Apktool/issues/520))
 * Fixed issue with `AndroidManifest.xml` missing attributes. ([Issue 623](https://github.com/iBotPeaches/Apktool/issues/623))
 * Fixed issue with ignoring `formatted="false"` attribute in `<string-array>`'s. ([Issue 786](https://github.com/iBotPeaches/Apktool/issues/786))
 * Fixed issue with multiple overlapping try catches. ([Issue 748](https://github.com/iBotPeaches/Apktool/issues/784))
 * Fixed issue with apks with multiple `ResPackages` where default is not `pkgId` 0. ([Issue 793](https://github.com/iBotPeaches/Apktool/issues/793))
 * Fixed issue with renamed manifest (`yi`). ([Issue 791](https://github.com/iBotPeaches/Apktool/issues/791))
 * Fixed issue with apks with large StringPools failing to decode. ([Issue 773](https://github.com/iBotPeaches/Apktool/issues/773))
 * Fixed issue with bad casting of `ResStringValue` to `ResAttr`. ([Issue 587](https://github.com/iBotPeaches/Apktool/issues/587)) / (Thanks whydoubt)
 * Fixed issue with hardcoding 9 patches as `.png` when there are `.qmg`, `.spi`. ([Issue 798](https://github.com/iBotPeaches/Apktool/issues/798))
 * Added support for Android 5.0 (Lollipop). ([Issue 763](https://github.com/iBotPeaches/Apktool/issues/763))
 * Added support for `TYPE_DYNAMIC_REFERENCE`. ([Issue 815](https://github.com/iBotPeaches/Apktool/issues/815))
 * Fixed issue with implicitly added version qualifiers. ([Issue 823](https://github.com/iBotPeaches/Apktool/issues/823))
 * Added support for shared library apks. ([Issue 822](https://github.com/iBotPeaches/Apktool/issues/822))
 * Fixed issue improperly casting strings that resembled filepaths to `ResFileValues`. ([Issue 440](https://github.com/iBotPeaches/Apktool/issues/440))
 * Fixed issue with segfaulting `aapt`. ([Issue 700](https://github.com/iBotPeaches/Apktool/issues/700))
 * Fixed issue with undefined attributes. ([Issue 655](https://github.com/iBotPeaches/Apktool/issues/655))
 * Fixed issue with improper handling of `MNC_ZERO` which caused duplicated resources. ([Issue 811](https://github.com/iBotPeaches/Apktool/issues/811))
 * Fixed warnings of "Cleaning up unclosed ZipFile...". ([Issue 853](https://github.com/iBotPeaches/Apktool/issues/853))
 * Added support for downloading gradle binaries over `https`. ([Issue 866](https://github.com/iBotPeaches/Apktool/issues/866))
 * Fixed issue when user has no access to `$HOME`. ([Issue 513](https://github.com/iBotPeaches/Apktool/issues/513))
 * Added support for `BCP-47` localization tags. ([Issue 870](https://github.com/iBotPeaches/Apktool/issues/870))
 * Fixed issue with double escaping of ampersands in `<`. ([Issue 658](https://github.com/iBotPeaches/Apktool/pull/105)) / (Thanks jhornber)
 * Fixed issue with not respecting compression type of unknown files. ([Issue 878](https://github.com/iBotPeaches/Apktool/issues/878)) / (Thanks simtel12) 
 * Fixed issue with apktool branding apks via `platformBuildVersion[Code/Name]`. ([Issue 890](https://github.com/iBotPeaches/Apktool/issues/890))
 * Fixed issue when multiple dex files were ignored using `-s`. ([Issue 904](https://github.com/iBotPeaches/Apktool/issues/904))
 * Fixed issue with `@string` references in `<provider>` attributes from preventing apk install. ([Issue 636](https://github.com/iBotPeaches/Apktool/issues/636))
 * Fixed issue with decoding `.spi` files as 9 patch images. (Thanks Furniel)
 * Fixed issue with APKs with multiple dex files. 
 * Fixed issue using Apktool without smali/baksmali.
 * Fixed issue using non URI standard characters in apk name. (Thanks rover12421)
 * Added version output during decode/build operations to quickly identify apktool version.
 * Fixed NPE error when using `.odex` files with `--no-src` specified. (Thanks Rodrigo Chiossi)
 * Fixed locale problems when locale changes meaning of windows `.bat` script. (Thanks Adem666)
 * Fixed issue when `-r` was used with no `/res` folder present. (Thanks chrisch1974)

## v1.5.2
2013.02.02

  * output smali filename errors to screen during rebuild instead of filestream ([Issue 410](https://github.com/iBotPeaches/Apktool/issues/410))
  * Only show the `--aapt / -a` info in verbose mode.
  * Don't crash out if .git folder isn't present. Use SNAPSHOT-DEV instead. ([Issue 503](https://github.com/iBotPeaches/Apktool/issues/503))
  * Only store compressed resources.arsc if original was compressed, otherwise STORE. ([Issue 178](https://github.com/iBotPeaches/Apktool/issues/178))
  * Moved build.gradle files to each sub-project for more organization
  * Prevented duplicated files in final jar which saved around 1.2mb. ([Issue 505](https://github.com/iBotPeaches/Apktool/issues/505))
  * Added Proguard to drop final jar size from 6.2mb to 2.6mb.
  * Added check for "aapt" in unit-tests. ([Issue 506](https://github.com/iBotPeaches/Apktool/issues/506))
  * Added ability to use `--frame-path` on `if|install-framework`
  * Fixed renaming of `.r.9.png` images that were incorrectly named to `.9.png`.
  * Added ability to use `--version` to print out Apktool version

## v1.5.1
2012.12.28

  * **Android 4.2 support**
  * Added -a / -aapt command on rebuild to specify location of aapt
  * Updated unit tests for 4.2 support
  * Closed file-handler when writing frameworks to file system.
  * Updated to Gradle 1.3
  * Properly deleted tmp files after building apk ([Issue 476](https://github.com/iBotPeaches/Apktool/issues/476))
  * Added support for renamed packages via `--renamed-manifest-package` ([Issue 363](https://github.com/iBotPeaches/Apktool/issues/363))
  * Option to specify framework folder ([Issue 286](https://github.com/iBotPeaches/Apktool/issues/286))
  * Prevents removal of configChanges in AndroidManifest ([Issue 415](https://github.com/iBotPeaches/Apktool/issues/415))
  * Updated snakeyaml to 1.11  to fix "unacceptable character" errors. ([Issue 471](https://github.com/iBotPeaches/Apktool/issues/471))
  * Updated smali/baksmali to `v1.4.1`
  * Fixed reference-array problem ( pull request [#53](https://github.com/iBotPeaches/Apktool/pull/53)))
  * Fixed bad spacing issue on Mac OS X (pull request [#49](https://github.com/iBotPeaches/Apktool/pull/49))
  * Removed maven in favor of gradle
  * Removed Maven REPOs that were used if local projects didn’t exist.
  * Merged brut.j.dir,brut.j.common,brut.j.util & brut.apktool.smali (Fork of JesusFreke’s smali) into one repo.
  * Fix –verbose mode to actually work
  * Added SDK API 17 framework

## v1.5.0
2012.09.02

  * Fix for colours being decompiled with wrong hex code
  * Fix for `<string-array>` being treated as `<array>`
  * Support for Mac OS X user:home instead of /home
  * updated builtin framework to SDK API16
  * Added `<user-sdk>` into apktool.yml for future aapt changes
  * Added `--verbose` commands to output contents from aapt (help with debugging)
  * Ignore bootclasspath on debug mode

## v1.4.10
2012.08.21

  * Fix for bad whitespace in manifest
  * Fix for bad decompilation of some apks (Credit: KOJAN)

## v1.4.9
2012.07.28

  * Fix for plurals.xml (Credit: MIUIRussia)
  * Added xhdpi configurations
  * Added uimodes configurations - miui roms
  * Fixed problem escaping chars like `' /`

## v1.4.8
2012.07.08

  * Revert greyscale fix due to errors
  * Skip extra com.htc package in some apks.
  * Some fixes from yyj inserted

## v1.4.7
2012.07.05

  * updated to baksmali/smali to public `v1.3.3`
  * Fix for grayscaled images (Credit: Charles)
  * Adjusted resources/configurations for API13

## v1.4.6
2012.02.14

  * Fixed reading of xlif data to correctly insert `formatted="false"`
  * Fixed `<item>` being read incorrectly and placed as attr type

## v1.4.5
2012.01.07

  * updated builtin framework to SDK API15
  * updated baksmali/smali to `v1.3.3dev`

## v1.4.4
2011.12.11

  * updated baksmali/smali to `v1.3.3`
  * New developer: iBotPeaches

## v1.4.3
2011.12.08

  * updated builtin framework to SDK API14
  * fixed some `"Multiple substitutions (...)"` errors ([Issue 365](https://github.com/iBotPeaches/Apktool/issues/365))

## v1.4.2
2011.12.02

  * added support for API14 (Android 4.0) resources
  * updated smali to `v1.3.0`
  * added `--quiet` option
  * fixed decoding error when string ends with '%' ([Issue 280](https://github.com/iBotPeaches/Apktool/issues/280))
  * fixed decoding error when `<plurals>` contains a reference ([Issue 345](https://github.com/iBotPeaches/Apktool/issues/345))
  * fixed a broken res when decoding `<array>` with positional substitutions ([Issue 333](https://github.com/iBotPeaches/Apktool/issues/333))

## v1.4.1
2011.05.15

  * fixed builtin framework. See changelog and migration instructions of v1.4.0 below.

## v1.4.0
2011.05.15

  * added Honeycomb support (3.1, API 12).
    * support for mipmaps and xlarge, xhdpi resource qualifiers
    * updated builtin framework to 3.1 (API 12)
  * completely rewritten mechanism of enclosing/escaping strings in XML files:
    * fixed legendary [Issue 211](https://github.com/iBotPeaches/Apktool/issues/211)
    * fixed a lot of other bugs discovered when writing integration tests
    * string format is simpler and more compact now: `"  "` instead of `\u0020 `, `\"` instead of `\&quot;`, etc.
  * fixed incompatibilities between newer aapt and apks built by older one. New aapt is more restrictive, some apps can't be built using it, even if you would have sources. Apktool tries to convert/fix these incompatibilities: it adds formatted="false" for `<string />` tags and enumerates substitutions for plurals.
  * updated smali to `v1.2.6`
  * added automatic integration tests for resource decoding and building
  * first official release of apktool built using Maven from open source
  * sort framework ids before storing them in apktool.yml. aapt command requires you to include frameworks in order.
  * zero-padding of MCC resource qualifier
  * prefer to use raw values when decoding XML attrs. This could make decoded XMLs a little more similar to original ones.
  * close apktool.yml file handler after generating it
  * added error message about missing input file when decoding
  * added more info messages during decoding.
  * do not decode res-references as `<item />`. Now they're decoded as e.g. `<string name="test">@android:string/ok</string>` instead of `<item type="string" name="test">@android:string/ok</item>`.
  * changed encoding of generated XML files from UTF-8 to utf-8 - it's more consistent with Android SDK.
  * add new line at the end of generated XML files.

### Migration to 1.4.0

In order to get full Honeycomb/Gingerbread (3.1, API 12) support you have to:

  * update apktool to `v1.4.0`
  * update install package to `r04-brut1` or update your aapt manually
  * then remove `$HOME/apktool/framework/1.apk` or install your own framework from Honeycomb/Gingerbread

## v1.3.2
2010.09.03

  * contains critical bug: ([Issue 211](https://github.com/iBotPeaches/Apktool/issues/211)).
  * updated smali to `v1.2.4`
  * added support for API Level 8 resource qualifiers: night, car, etc. ([Issue 176](https://github.com/iBotPeaches/Apktool/issues/176))
  * added support for broken file-resources ([Issue 202](https://github.com/iBotPeaches/Apktool/issues/202))
  * don't generate sdkVersion (`"-v"`) resource qualifiers if they're "natural" ([Issue 196](https://github.com/iBotPeaches/Apktool/issues/196))
  * always compress resources.arsc file ([Issue 178](https://github.com/iBotPeaches/Apktool/issues/178))
  * throw warnings instead of exceptions on unknown files inside smali dir ([Issue 188](https://github.com/iBotPeaches/Apktool/issues/188))
  * added support for resources using invalid/unknown config flags, e.g. from future APIs or added by manufacturer ([Issue 176](https://github.com/iBotPeaches/Apktool/issues/176))
  * added an option to keep broken resources to fix them manually ([Issue 176](https://github.com/iBotPeaches/Apktool/issues/176))
  * fixed case-sensitivity problems ([Issue 197](https://github.com/iBotPeaches/Apktool/issues/197))
  * fixed an issue when `*`.9.png doesn't have 9patch chunk in it ([Issue 170](https://github.com/iBotPeaches/Apktool/issues/170))
  * fixed NPE when there is a file without extension in drawable dir ([Issue 173](https://github.com/iBotPeaches/Apktool/issues/173))
  * fixed escaping of chars in XML style tags ([Issue 175](https://github.com/iBotPeaches/Apktool/issues/175))
  * fixed an error, when there are missing resources in a type, which does not have default config (http://forum.xda-developers.com/showthread.php?p=7949440#post7949440)
  * try to use original value of XML attribute - instead of parsed one. Fixes an issue when apktool was decoding e.g. "01" as "1" ([Issue 187](https://github.com/iBotPeaches/Apktool/issues/187))
  * added more debugging info when omitting unknown config flags (`"Config size > 32" message`)

## v1.3.1
2010.06.14

  * added decoding of XML tags in res strings ([Issue 125](https://github.com/iBotPeaches/Apktool/issues/125))
  * fixed some issues ([Issue 155](https://github.com/iBotPeaches/Apktool/issues/155)), ([Issue 156](https://github.com/iBotPeaches/Apktool/issues/156)), ([Issue 158](https://github.com/iBotPeaches/Apktool/issues/158)), ([Issue 167](https://github.com/iBotPeaches/Apktool/issues/167))

## v1.3.0
2010.06.12

  * added 9patch images (`*`.9.png) decoding ([Issue 112](https://github.com/iBotPeaches/Apktool/issues/112))
  * fixed support for apks lacking res dir or resources.arsc file ([Issue 154](https://github.com/iBotPeaches/Apktool/issues/154), [Issue 160](https://github.com/iBotPeaches/Apktool/issues/160))
  * changed default name of built file from out.apk to `<original\_name.apk>`
  * added possibility to choose location of output apk when building

## v1.2.0
2010.06.03

  * added feature for installing and using custom framework files - pulled from a device. See FrameworkFiles ([Issue 137](https://github.com/iBotPeaches/Apktool/issues/137))
  * Froyo support ([Issue 147](https://github.com/iBotPeaches/Apktool/issues/147))
  * updated Android framework to `2.2r1`
  * removed HTC framework
  * no need to use `-s/-r` switch, when apk doesn't contain sources/resources
  * added protection against accidental remove of important files when decoding ([Issue 146](https://github.com/iBotPeaches/Apktool/issues/146))
  * made outdir argument optional when decoding
  * fix: adding dummy resources if some of them are missing ([Issue 150](https://github.com/iBotPeaches/Apktool/issues/150))
  * fix: better recognition of AXML files ([Issue 151](https://github.com/iBotPeaches/Apktool/issues/151))

## v1.1.1
2010.04.29

  * much better recognition of register types when debugging ([Issue 134](https://github.com/iBotPeaches/Apktool/issues/134))
  * fixed SIGSEGV error, which usually occurred after several steps when debugging ([Issue 136](https://github.com/iBotPeaches/Apktool/issues/136))

## v1.1.0
2010.04.28

  * added smali debugging! For more info, see: SmaliDebugging
  * added HTC resources ([Issue 127](https://github.com/iBotPeaches/Apktool/issues/127))
  * made all framework resources public ([Issue 126](https://github.com/iBotPeaches/Apktool/issues/126))
  * proper escaping of whitespaces in resource strings ([Issue 124](https://github.com/iBotPeaches/Apktool/issues/124))
  * updated (bak)smali to 1.2.2
  * fixed "Building resources..." freeze bug ([Issue 123](https://github.com/iBotPeaches/Apktool/issues/123))

## v1.0.0
2010.04.02

  * it is now pure-Java, so should work on any platform
  * added support for decoding and rebuilding framework-res.apk file
  * added support for many new resources features and apks
  * included automatic workaround for 9-patch images issue
  * added possibility to decode sources or resources only
  * automatic detecting, whether there is a need for rebuilding files
  * added some info messages
  * it is less invasive, cause res ids in built file are identical as in original apk
  * it is much faster
  * fixed a lot of bugs

## v0.9.2
2010.03.13

  * many, many new apps supported. I've tested it on 85 random apps pulled from my device: 0.9.1 gave me 28 failures, 0.9.2 only 2. This does not mean all 83 apps are perfectly fine, but it is quite probably that they are.
  * smali, baksmali and android resources are now builtin, so it is much simpler to install apktool. Still you need aapt in a PATH.
  * much better error reporting
  * added simple usage help
  * new "bs" command

## v0.9.1
2010.03.02

  * [#111](https://github.com/iBotPeaches/Apktool/issues/111) fixed
  * fixed support for many apps
