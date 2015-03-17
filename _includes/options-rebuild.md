These are all the options when building an apk.
<br /><br />
<strong><code>-a, --aapt &lt;FILE>></code></strong>
<blockquote>Loads aapt from the specified file location, instead of relying on path. Falls back to <code>$PATH</code> loading, if no file found</blockquote>
<br />
<strong><code>-c, --copy-original</code></strong>
<blockquote>Copies original <code>AndroidManifest.xml</code> and <code>META-INF</code> folder into built apk</blockquote>
<br />
<strong><code>-d, --debug</code></strong>
<blockquote>Builds in debug mode. see <a href="#smali-debugging">SmaliDebugging</a> for more information</blockquote>
<br />
<strong><code>-f, --force-all</code></strong>
<blockquote>Overwrites existing files during build, reassembling the <code>resources.arsc</code> file and <code>classes.dex</code> file</blockquote>
<br />
<strong><code>-o, --output &lt;DIR></code></strong>
<blockquote>The name and location of the apk that gets written</blockquote>
<br />
<strong><code>-p, --frame-path &lt;DIR></code></strong>
<blockquote>The location where framework files are loaded from</blockquote>
<br />