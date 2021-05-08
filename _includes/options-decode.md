These are all the options when decoding an apk.
<br /><br />
<strong><kbd>-api, --api-level &lt;API></kbd></strong>
<blockquote>The numeric api-level of the smali files to generate (defaults to targetSdkVersion)</blockquote>
<br />
<strong><kbd>-b, --no-debug-info</kbd></strong>
<blockquote>Prevents baksmali from writing out debug info (.local, .param, .line, etc). Preferred to use if you are comparing smali from the same APK of different versions. The line numbers and debug will change among versions, which can make DIFF reports a pain.</blockquote>
<br />
<strong><kbd>-f, --force</kbd></strong>
<blockquote>Force delete destination directory. Use when trying to decode to a folder that already exists</blockquote>
<br />
<strong><kbd>--force-manifest</kbd> - <span class="label label-success">v2.3.1</span> - <span class="label label-info">Used for analysis</span></strong>
<blockquote>Forces decode of AndroidManifest regardless of decoding of resources flag. Will more than likely prevent rebuild as used for static analysis of the manifest.</blockquote>
<br />
<strong><kbd>--keep-broken-res</kbd></strong> - <span class="label label-danger">Advanced</span>
<blockquote>If there is an error like "Invalid Config Flags Detected. Dropping Resources...". This means that APK has a different structure then Apktool can handle. This might be a newer Android version or a random APK that doesn't match standards. Running this will allow the decode, but then you have to manually fix the folders with -ERR in them.</blockquote>
<br />
<strong><kbd>-m, --match-original</kbd></strong> - <span class="label label-info">Used for analysis</span>
<blockquote>Matches files closest as possible to original, but <strong>prevents</strong> rebuild.</blockquote>
<br />
<strong><kbd>--no-assets</kbd> - <span class="label label-success">v2.3.0</span></strong>
<blockquote>Prevents decoding/copying of unknown asset files.</blockquote>
<br />
<strong><kbd>-o, --output &lt;DIR></kbd></strong>
<blockquote>The name of the folder that apk gets written to</blockquote>
<br />
<strong><kbd>--only-main-classes</kbd> - <span class="label label-success">v2.4.1</span></strong>
<blockquote>Only disasemble dex classes in root (classes[0-9].dex)</blockquote>
<br />
<strong><kbd>-p, --frame-path &lt;DIR></kbd></strong>
<blockquote>The folder location where framework files should be stored/read from</blockquote>
<br />
<strong><kbd>-r, --no-res</kbd></strong>
<blockquote>This will prevent the decompile of resources. This keeps the <kbd>resources.arsc</kbd> intact without any decode. If only editing Java (smali) then this is the recommended action for faster decompile & rebuild</blockquote>
<br />
<strong><kbd>-s, --no-src</kbd></strong>
<blockquote>This will prevent the disassembly of the dex file(s). This keeps the apk <kbd>dex</kbd> file(s) and simply moves it during build. If you are only editing the resources. This is the recommended action for faster disassemble & assemble</blockquote>
<br />
<strong><kbd>-t, --frame-tag &lt;TAG></kbd></strong>
<blockquote>Uses framework files tagged via <kbd>&lt;TAG></kbd></blockquote>
