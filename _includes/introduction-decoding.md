The decode option on Apktool can be invoked either from <code>d</code> or <code>decode</code> like shown below.
{% highlight console %}
$ apktool d foo.jar
// decodes foo.jar to foo.jar.out folder

$ apktool decode foo.jar
// decodes foo.jar to foo.jar.out folder

$ apktool d bar.apk
// decodes bar.apk to bar folder

$ apktool decode bar.apk
// decodes bar.apk to bar folder

$ apktool d bar.apk -o baz
// decodes bar.apk to baz folder
{% endhighlight %}