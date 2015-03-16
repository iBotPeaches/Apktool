These are all the options when decoding an APK.
<br /><br />
<strong><code>--api &lt;API></code></strong>
<blockquote>The numeric api-level of the smali files to generate (defaults to targetSdkVersion)</blockquote>
<br />
<strong><code>-b, --no-debug-info</code></strong>
<blockquote>Prevents baksmali from writing out debug info (.local, .param, .line, etc). Preferred to use if you are comparing smali from the same APK of different versions. The line numbers and debug will change among versions, which can make DIFF reports a pain.</blockquote>
<br />
<strong><code>-d, --debug</code></strong>
<blockquote>Decodes in Debug Mode. Read <a href="#SmaliDebugging">SmaliDebugging</a></blockquote>