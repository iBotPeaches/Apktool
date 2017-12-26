These are all the options when building an apk.
<br /><br />
<strong><kbd>-a, --aapt &lt;FILE></kbd></strong>
<blockquote>Loads aapt from the specified file location, instead of relying on path. Falls back to <kbd>$PATH</kbd> loading, if no file found</blockquote>
<br />
<strong><kbd>-c, --copy-original</kbd></strong> - <span class="label label-danger">Will still require signature resign post API18</span>
<blockquote>Copies original <kbd>AndroidManifest.xml</kbd> and <kbd>META-INF</kbd> folder into built apk</blockquote>
<br />
<strong><kbd>-d, --debug</kbd></strong>
<blockquote>Adds <kbd>debuggable="true"</kbd> to AndroidManifest file.</blockquote>
<br />
<strong><kbd>-f, --force-all</kbd></strong>
<blockquote>Overwrites existing files during build, reassembling the <kbd>resources.arsc</kbd> file and <kbd>dex</kbd> file(s)</blockquote>
<br />
<strong><kbd>-o, --output &lt;FILE></kbd></strong>
<blockquote>The name and location of the apk that gets written</blockquote>
<br />
<strong><kbd>-p, --frame-path &lt;DIR></kbd></strong>
<blockquote>The location where framework files are loaded from</blockquote>
<br />