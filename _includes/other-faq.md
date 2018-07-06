<br />
<strong>What about the <code>-j</code> switch shown from the original YouTube videos?</strong>
<br />
Read <a href="https://github.com/iBotPeaches/Apktool/issues/199">Issue 199</a>. In short - it doesn't exist.
<br /><br />
<strong>Is it possible to run apktool on a device?</strong>
<br />
Sadly not. There are some incompatibilities with running the <code>aapt</code> binary on the device. Additionally, older builds of Android have problems with <code>java.nio</code>.
<br /><br />
<strong>Where can I download sources of apktool?</strong>
<br />
From our <a target="_blank" href="https://github.com/iBotPeaches/Apktool">Github</a> or <a target="_blank" href="https://bitbucket.org/iBotPeaches/apktool/overview">Bitbucket</a> project.
<br /><br />
<strong>Resulting apk file is much smaller than original! Is there something missing?</strong>
<br />
There are a couple of reasons that might cause this.
<ul>
  <li>Apktool builds unsigned apks. This means an entire directory <code>META-INF</code> is missing.</li>
  <li>New aapt binary. Newer versions of apktool contain a newer aapt which optimizes images differently.</li>
</ul>
These points might have contributed to a smaller than normal apk
<br /><br />
<strong>There is no META-INF dir in resulting apk. Is this ok?</strong>
<br />
Yes. <code>META-INF</code> contains apk signatures. After modifying the apk it is no longer signed. You can use <code>-c / --copy-original</code> to retain these signatures. However,
using <code>-c</code> uses the original <code>AndroidManifest.xml</code> file, so changes to it will be lost.
<br /><br />
<strong>What do you call "magic apks"?</strong>
<br />
For some reason there are apks that are built using modified build tools. These apks don't work on a regular AOSP Android build, but usually are accompanied
by a modified system that can read these modified apks. Apktool cannot handle these apks, therefore they are "magic".
<br /><br />
<strong>Could I integrate apktool into my own tool? Could I modify apktool sources? Do I have to credit you?</strong>
<br />
Actually the Apache License, which apktool uses, answers all these questions. Yes you can redistribute and/or modify apktool without my permission. However,
if you do it would be nice to add our contributors (brut.all, iBotPeaches and JesusFreke) into your credits but it's not required.
<br /><br />
<strong>Where does apktool store its framework files?</strong>
<br />
<ul>
  <li><strong>unix</strong> - <pre>$HOME/.local/share/apktool</pre></li>
  <li><strong>mac</strong> - <pre>$HOME/Library/apktool</pre></li>
  <li><strong>windows</strong> - <pre>$HOME/AppData/Local/apktool</pre></li>
</ul>
<br /><br />
