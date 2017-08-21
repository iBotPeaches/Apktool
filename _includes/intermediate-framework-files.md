As you probably know, Android apps utilize code and resources that are found on the Android OS itself. These are known as framework resources and Apktool relies on these
to properly decode and build apks.
<br /><br />
Every Apktool release contains internally the most up to date AOSP framework at the time of the release. This allows you to decode and build most apks without a problem.
However, manufacturers add their own framework files in addition to the regular AOSP ones. To use apktool against these manufacturer apks you must first install the 
manufacturer framework files.
<h4><strong>Example</strong></h4>
Lets say you want to decode <code>HtcContacts.apk</code> from an HTC device. If you try you will get an error message.
{% highlight console %}
$ apktool d HtcContacts.apk
I: Loading resource table...
I: Decoding resources...
I: Loading resource table from file: 1.apk
W: Could not decode attr value, using undecoded value instead: ns=android, name=drawable
W: Could not decode attr value, using undecoded value instead: ns=android, name=icon
Can't find framework resources for package of id: 2. You must install proper framework files, see project website for more info.
{% endhighlight %}

We must get HTC framework resources before decoding this apk. We pull <code>com.htc.resources.apk</code> from our device and install it
{% highlight console %}
$ apktool if com.htc.resources.apk
I: Framework installed to: 2.apk
{% endhighlight %}

Now we will try this decode again.
{% highlight console %}
$ apktool d HtcContacts.apk 
I: Loading resource table...
I: Decoding resources...
I: Loading resource table from file: /home/brutall/apktool/framework/1.apk
I: Loading resource table from file: /home/brutall/apktool/framework/2.apk
I: Copying assets and libs...
{% endhighlight %}

As you can see. Apktool leveraged both <code>1.apk</code> and <code>2.apk</code> framework files in order to properly decode this application.

<h4><strong>Finding Frameworks</strong></h4>
For the most part any apk in <code>/system/framework</code> on a device will be a framework file. On some devices they might reside in
<code>/data/system-framework</code> and even cleverly hidden in <code>/system/app</code> or <code>/system/priv-app</code>. They are usually
named with the naming of "resources", "res" or "framework".
<br /><br />
<blockquote class="info"><span class="label label-info lb">Example</span> HTC has a framework called <code>com.htc.resources.apk</code>, LG has one called <code>lge-res.apk</code></blockquote>

After you find a framework file you could pull it via <kbd>adb pull /path/to/file</kbd> or use a file manager application. After you have the
file locally, pay attention to how Apktool installs it. The number that the framework is named during install corresponds to the pkgId of the
application. These values should range from 1 to 30. Any APK that installs itself as <code>127</code> is <code>0x7F</code> which is an internal pkgId.

<h4><strong>Internal Frameworks</strong></h4>
Apktool comes with an internal framework like mentioned above. This file is copied to <code>$HOME/apktool/framework/1.apk</code> during use.
<br /><br />

<blockquote class="warning"><span class="label label-warning lb">Warning</span> Apktool has no knowledge of what version of framework resides there. It will assume its up to date, so delete the file during Apktool upgrades</blockquote>
<h4><strong>Managing framework files</strong></h4>
Frameworks are stored in different places depending on the OS in question.
<ul>
    <li>unix - <code>$HOME/.local/share/apktool</code></li>
    <li>windows - <code>%UserProfile%\AppData\Local\apktool</code></li>
    <li>mac - <code>$HOME/Library/apktool</code></li>
</ul>

If these directories are not available it will default to <code>java.io.tmpdir</code> which is usually <code>/tmp</code>.
This is a volatile directory so it would make sense to take advantage of the parameter <code>--frame-path</code> to select an alternative folder for framework files.
<br /><br />
Since these locations are in sometimes hidden directories, managing these frameworks becomes a challenge. A simple helper function
(added in v2.2.1) allows you to run <code>apktool empty-framework-dir</code> to empty out frameworks.
<br /><br />

<blockquote class="info"><span class="label label-info lb">Note</span> Apktool has no control over the frameworks once installed, but you are free to manage these files on your own.</blockquote>
<h4><strong>Tagging framework files</strong></h4>
Frameworks are stored in the naming convention of: <code>&lt;id>-&lt;tag>.apk</code>. They are identified by pkgId and optionally custom tag. Usually tagging frameworks isn't necessary, but if you work on apps from many different devices and they have incompatible frameworks, you will need some way to easily switch between them.

You could tag frameworks by:

{% highlight console %}
$ apktool if com.htc.resources.apk -t hero
I: Framework installed to: /home/brutall/apktool/framework/2-hero.apk
$ apktool if com.htc.resources.apk -t desire
I: Framework installed to: /home/brutall/apktool/framework/2-desire.apk
{% endhighlight %}

Then:

{% highlight console %}
$ apktool d HtcContacts.apk -t hero
I: Loading resource table...
I: Decoding resources...
I: Loading resource table from file: /home/brutall/apktool/framework/1.apk
I: Loading resource table from file: /home/brutall/apktool/framework/2-hero.apk
I: Copying assets and libs...
$ apktool d HtcContacts.apk -t desire
I: Loading resource table...
I: Decoding resources...
I: Loading resource table from file: /home/brutall/apktool/framework/1.apk
I: Loading resource table from file: /home/brutall/apktool/framework/2-desire.apk
I: Copying assets and libs...
{% endhighlight %}
You don't have to select a tag when building apk - apktool automatically uses the same tag, as when decoding.
