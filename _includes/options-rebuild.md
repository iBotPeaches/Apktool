These are all the options when building an apk.
<br /><br />
<strong><kbd>-a, --aapt &lt;FILE></kbd></strong>
<blockquote>Loads aapt from the specified file location, instead of relying on path. Falls back to <kbd>$PATH</kbd> loading, if no file found. Unless <kbd>$PATH</kbd> references prebuilt custom aapt. This will more than likely not work.</blockquote>
<br />
<strong><kbd>-api, --api-level &lt;API></kbd> - <span class="label label-success">v2.4.0</span></strong>
<blockquote>The numeric api-level of the smali files to build against (defaults to minSdkVersion)</blockquote>
<br />
<strong><kbd>-c, --copy-original</kbd> - <span class="label label-danger">Removal - v3.0.0</span></strong>
<blockquote>Copies original <kbd>AndroidManifest.xml</kbd> and <kbd>META-INF</kbd> folder into built apk. Scheduled for <strong>deprecation</strong>.</blockquote>
<br />
<strong><kbd>-d, --debug</kbd> - <span class="label label-success">v2.4.0</span></strong>
<blockquote>Adds <kbd>debuggable="true"</kbd> to AndroidManifest file.</blockquote>
<br />
<strong><kbd>-f, --force-all</kbd></strong>
<blockquote>Overwrites existing files during build, reassembling the <kbd>resources.arsc</kbd> file and <kbd>dex</kbd> file(s)</blockquote>
<br />
<strong><kbd>-nc,--no-crunch</kbd> - <span class="label label-success">v2.4.0</span></strong>
<blockquote>Disable crunching of resource files during the build step.</blockquote>
<br />
<strong><kbd>-o, --output &lt;FILE></kbd></strong>
<blockquote>The name and location of the apk that gets written</blockquote>
<br />
<strong><kbd>-p, --frame-path &lt;DIR></kbd></strong>
<blockquote>The location where framework files are loaded from</blockquote>
<br />
<strong><kbd>--use-aapt2</kbd> - <span class="label label-success">v2.3.2</span></strong>
<blockquote>Use the aapt2 binary instead of appt</blockquote>
<br />
