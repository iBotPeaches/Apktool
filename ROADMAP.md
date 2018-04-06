## Automatic Remapping of ResourceId
We currently prevent resourceIds from changing, by utilizing the `public.xml` file which makes the resources public, but
then prevents them to be used in some locations (`android:scheme`). The correct fix would be to record the resourceIds
and use dexlib2 (no regular expressions) to rewrite them to the new resourceId after the `resources.arsc` is built.

This would be a lookup table of old->new resourceIds leveraging the API of dexlib2 to do the replacement. Doing this
properly would nullify the need to do [#191](https://github.com/iBotPeaches/Apktool/issues/191)

Suggestions: [#244](https://github.com/iBotPeaches/Apktool/issues/244)