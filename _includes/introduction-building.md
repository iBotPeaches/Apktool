The build option can be invoked either from <code>b</code> or <code>build</code> like shown below
{% highlight console %}
$ apktool b foo.jar.out
// builds foo.jar.out folder into foo.jar.out/dist/foo.jar file

$ apktool build foo.jar.out
// builds foo.jar.out folder into foo.jar.out/dist/foo.jar file

$ apktool b bar
// builds bar folder into bar/dist/bar.apk file

$ apktool b .
// builds current directory into ./dist

$ apktool b bar -o new_bar.apk
// builds bar folder into new_bar.apk

$ apktool b bar.apk
// WRONG: brut.androlib.AndrolibException: brut.directory.PathNotExist: apktool.yml
// Must use folder, not apk/jar file

{% endhighlight %}

<blockquote class="info"><span class="label label-info lb">Info</span> In order to run a rebuilt application. You must resign the application.
Android <a target="_blank" href="https://developer.android.com/tools/publishing/app-signing.html#signing-manually">documentation</a> can help with this.</blockquote>
