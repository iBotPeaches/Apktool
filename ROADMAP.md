## Automatic Remapping of ResourceId
We currently prevent resourceIds from changing, by utilizing the `public.xml` file which makes the resources public, but
then prevents them to be used in some locations (`android:scheme`). The correct fix would be to record the resourceIds
and use dexlib2 (no regular expressions) to rewrite them to the new resourceId after the `resources.arsc` is built.

This would be a lookup table of old->new resourceIds leveraging the API of dexlib2 to do the replacement. Doing this
properly would nullify the need to do [#191](https://github.com/iBotPeaches/Apktool/issues/191)

Suggestions: [#244](https://github.com/iBotPeaches/Apktool/issues/244)

## Implicit Qualifiers Cleanup
Currently we have a mismatch between reading the folders and reading the qualifiers which leads to a mismatch between
implicit qualifiers like version (-v4, v13, etc).

This was first spotted in bug [#1272](https://github.com/iBotPeaches/Apktool/issues/1272).

This was attempted to be fixed in [!1758](https://github.com/iBotPeaches/Apktool/pull/1758/files), but had to be
reverted due to [this](https://github.com/iBotPeaches/Apktool/issues/1272#issuecomment-379345005).

Suggestions: [#2237](https://github.com/iBotPeaches/Apktool/issues/2237)