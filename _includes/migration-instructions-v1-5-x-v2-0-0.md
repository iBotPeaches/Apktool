<ul>
  <li><strong>Java 1.7 is required</strong></li>
  <li>Update apktool to v2.0.0</li>
  <li>aapt is now included inside the apktool binary. It's not required to maintain your own aapt install under $PATH. (However, features like <code>-a / --aapt</code> are still used and can override the internal aapt)</li>
  <li>The addition of aapt replaces the need for separate aapt download packages. Helper Scripts may be found <a href="https://github.com/iBotPeaches/Apktool/tree/master/scripts">here</a></li>
  <li>Remove framework <code>$HOME/apktool/framework/1.apk</code></li>
  <li>Eagle eyed users will notice resources are now decoded before sources now. This is because we need to know the API version via the manifest for decoding the sources</li>
</ul>
<br />
<strong>Parameter Changes</strong>
<ul>
  <li>Smali/baksmali 2.0 are included. This is a big change from 1.4.2. Please read the smali updates <a href="https://code.google.com/p/smali/wiki/SmaliBaksmali20">here</a> for more information</li>
  <li><code>-o / --output</code> is now used for the output of apk/directory</li>
  <li><code>-t / --tag</code> is required for tagging framework files</li>
  <li><code>-advance / --advanced</code> will launch advance parameters and information on the usage output</li>
  <li><code>-m / --match-original</code> is a new feature for apk analysis. This retains the apk is nearly original format, but will make rebuild more than likely not work due to ignoring the changes that newer aapt requires</li>
  <li>After <code>[d]ecode</code>, there will be new folders (original / unknown) in the decoded apk folder</li>
  <ul>
    <li><strong>original</strong> = <code>META-INF folder</code> / <code>AndroidManifest.xml</code>, which are needed to retain the signature of apks to prevent needing to resign. Used with <code>-c / --copy-original</code> on <code>[b]uild</code></li>
    <li><strong>unknown</strong> = Files / folders that are not part of the standard AOSP build procedure. These files will be injected back into the rebuilt APK.</li>
  </ul>
  <li><code>apktool.yml</code> collects more information than last version</li>
  <ul>
    <li><code>SdkInfo</code> - Used to repopulate the sdk information in <code>AndroidManifest.xml</code> since newer aapt requires version information to be passed via parameter</li>
    <li><code>packageInfo</code> - Used to help support Android 4.2 renamed manifest feature. Automatically detects differences between resource and manifest and performs automatic <code>--rename-manifest-package</code> on <code>[b]uild</code></li>
    <li><code>versionInfo</code> - Used to repopulate the version information in <code>AndroidManifest.xml</code> since newer aapt requires version information to be passed via parameter</li>
    <li><code>compressionType</code> - Used to determine the compression that <code>resources.arsc</code> had on the original apk in order to replicate during <code>[b]uild</code></li>
    <li><code>unknownFiles</code> - Used to record name/location of non-standard files in an apk in order to place correctly on rebuilt apk</li>
    <li><code>sharedLibrary</code> - Used to help support Android 5 shared library feature by automatically detecting shared libraries and using <code>--shared-lib</code> on <code>[b]uild</code></li>
  </ul>
</ul>

<strong>Examples of new usage in 2.0 vs 1.5.x</strong>
<table class="table">
  <thead>
    <tr>
      <th>Old (Apktool 1.5.x)</th>
      <th>New (Apktool 2.0.x)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><kbd>apktool if framework-res.apk tag</kbd></td>
      <td><kbd>apktool if framework-res.apk -t tag</kbd></td>
    </tr>
    <tr>
      <td><kbd>apktool d framework-res.apk output</kbd></td>
      <td><kbd>apktool d framework.res.apk -o output</kbd></td>
    </tr>
    <tr>
      <td><kbd>apktool b output new.apk</kbd></td>
      <td><kbd>apktool b output -o new.apk</kbd></td>
    </tr>
  </tbody>
</table>