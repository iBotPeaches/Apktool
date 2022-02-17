<h4><strong>Android xml binary format</strong></h4>
Android Apks store its xml files in a binary format (you'll had noticed this if you have ever tried to read AndroidManifest.xml without the use of Apktool).<br /> <br />
This format is not described anywhere, but it uses some of the structures defined at <a target="_blank" href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/libs/androidfw/include/androidfw/ResourceTypes.h">ResourceTypes.h</a>
<br />
A great introduction to this format can be found <a target="_blank" href="https://justanapplication.wordpress.com/android-internals/">here</a>, below the Binary XML header.
<br />
<h4><strong>Android Manifest hacking</strong></h4>
As described <a target="_blank" href="https://maldr0id.blogspot.com/2014/09/having-fun-with-androidmanifestxml.html">here </a>, strings inside AndroidManifest.xml can be modified to confuse researchers, it migth be worth taking a look.
