The build option can be invoked either from <code>b</code> or <code>build</code> like shown below
{% highlight console %}
$ apktool b foo.out
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