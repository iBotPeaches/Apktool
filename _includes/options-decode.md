These are all the options when decoding an apk.
<br /><br />
<strong><code>--api &lt;API></code></strong>
<blockquote>The numeric api-level of the smali files to generate (defaults to targetSdkVersion)</blockquote>
<br />
<strong><code>-b, --no-debug-info</code></strong>
<blockquote>Prevents baksmali from writing out debug info (.local, .param, .line, etc). Preferred to use if you are comparing smali from the same APK of different versions. The line numbers and debug will change among versions, which can make DIFF reports a pain.</blockquote>
<br />
<strong><code>-f, --force</code></strong>
<blockquote>Force delete destination directory. Use when trying to decode to a folder that already exists</blockquote>
<br />
<strong><code>--keep-broken-res</code></strong> - <span class="label label-danger">Advanced</span>
<blockquote>If there is an error like "Invalid Config Flags Detected. Dropping Resources...". This means that APK has a different structure then Apktool can handle. This might be a newer Android version or a random APK that doesn't match standards. Running this will allow the decode, but then you have to manually fix the folders with -ERR in them.</blockquote>
<br />
<strong><code>-m, --match-original</code></strong> - <span class="label label-info">Used for analysis</span>
<blockquote>Matches files closest as possible to original, but <strong>prevents</strong> rebuild.</blockquote>
<br />
<strong><code>-o, --output &lt;DIR></code></strong>
<blockquote>The name of the folder that apk gets written to</blockquote>
<br />
<strong><code>-p, --frame-path &lt;DIR></code></strong>
<blockquote>The folder location where framework files should be stored/read from</blockquote>
<br />
<strong><code>-r, --no-res</code></strong>
<blockquote>This will prevent the decompile of resources. This keeps the <code>resources.arsc</code> intact without any decode. If only editing Java (smali) then this is the recommend for faster decompile & rebuild</blockquote>
<br />
<strong><code>-s, --no-src</code></strong>
<blockquote>This will prevent the disassemble of the dex files. This keeps the apk <code>classes.dex</code> file and simply moves it during build. If your only editing the resources. This is recommended for faster decompile & rebuild</blockquote>
<br />
<strong><code>-t, --frame-tag &lt;TAG></code></strong>
<blockquote>Uses framework files tagged via <code>&lt;TAG></code></blockquote>