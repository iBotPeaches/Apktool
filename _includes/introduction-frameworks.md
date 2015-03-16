Frameworks can be installed either from <code>if</code> or <code>install-framework</code>, in addition two parameters
<ul>
  <li><code>-p, --frame-path &lt;dir></code> - Store framework files into <code>&lt;dir></code> </li>
  <li><code>-t, --tag  &lt;tag></code> - Tag frameworks using <code>&lt;tag></code></li>
</ul>
Allow for a finer control over how the files are named and how they are stored.

{% highlight console %}
$ apktool if framework-res.apk
I: Framework installed to: 1.apk 
// pkgId of framework-res.apk determines number (which is 0x01)

$ apktool if com.htc.resources.apk
I: Framework installed to: 2.apk 
// pkgId of com.htc.resources is 0x02

$ apktool if com.htc.resources.apk -t htc
I: Framework installed to: 2-htc.apk 
// pkgId-tag.apk

$ apktool if framework-res.apk -p foo/bar
I: Framework installed to: foo/bar/1.apk

$ apktool if framework-res.apk -t baz -p foo/bar
I: Framework installed to: foo/bar/1-baz.apk
{% endhighlight %}