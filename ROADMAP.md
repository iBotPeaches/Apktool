## Automatic Remapping of ResourceId
We currently prevent resourceIds from changing, by utilizing the `public.xml` file which makes the resources public, but
then prevents them to be used in some locations (`android:scheme`). The correct fix would be to record the resourceIds
and use dexlib2 (no regular expressions) to rewrite them to the new resourceId after the `resources.arsc` is built.

This would be a lookup table of old->new resourceIds leveraging the API of dexlib2 to do the replacement. Doing this
properly would nullify the need to do [#191](https://github.com/iBotPeaches/Apktool/issues/191)

Suggestions: [#244](https://github.com/iBotPeaches/Apktool/issues/244)
Discussions: [#2062](https://github.com/iBotPeaches/Apktool/issues/2062)

## Implicit Qualifiers Cleanup
Currently we have a mismatch between reading the folders and reading the qualifiers which leads to a mismatch between
implicit qualifiers like version (-v4, v13, etc).

This was first spotted in bug [#1272](https://github.com/iBotPeaches/Apktool/issues/1272).

This was attempted to be fixed in [!1758](https://github.com/iBotPeaches/Apktool/pull/1758/files), but had to be
reverted due to [this](https://github.com/iBotPeaches/Apktool/issues/1272#issuecomment-379345005).

Suggestions: [#2237](https://github.com/iBotPeaches/Apktool/issues/2237)

## Qualifier Plugin System
For some OEMs, past and present. They re-use qualifiers that AOSP ends up using. This with CTS is becoming very
rare and pretty much a problem of the past, but now custom modifications and more "off the cuff" OEMs are doing
it.

Apktool can't do anything because it stays true to AOSP. It would need a plugin system that controls how to
read the qualifiers. Or even an override file.

Suggestions: [#1420](https://github.com/iBotPeaches/Apktool/issues/1420), [#2474](https://github.com/iBotPeaches/Apktool/issues/2474)

## Non-reference Resources
Some applications may shove resources into the /res folder, but have no references to them. Apktool follows
the resource table, so these files are effectively abandoned.

Crawling the filesystem for non-checked files would be slow especially having to cross check with already
parsed files.

Suggestions: [#1366](https://github.com/iBotPeaches/Apktool/issues/1366)

## Multi-threaded
Applications are getting larger as well as frameworks, but Apktool is getting slower.

Suggestions: [#2685](https://github.com/iBotPeaches/Apktool/issues/2685)

## Android Support
Folks have requested running Apktool on device itself. This has been a challenge due to the arch requirements
that would be placed on the aapt2/aapt binaries.

Suggestions: [#2811](https://github.com/iBotPeaches/Apktool/issues/2811)

## Split APK Support
Applications are further getting split on qualifiers. Apktool has been built on the assumption of one apk.

Suggestions: [#2283](https://github.com/iBotPeaches/Apktool/issues/2283), [#2218](https://github.com/iBotPeaches/Apktool/issues/2218), [#2880](https://github.com/iBotPeaches/Apktool/issues/2880)

## Dummy Resources
Folks want the ability to stop the auto generation of dummy resources.

Suggestions: [#2683](https://github.com/iBotPeaches/Apktool/issues/2683), [#2104](https://github.com/iBotPeaches/Apktool/issues/2104)
Pull Request(s): [#2463](https://github.com/iBotPeaches/Apktool/pull/2463)
