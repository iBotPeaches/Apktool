Docs exist for the mysterious 9patch images <a target="_blank" href="http://developer.android.com/guide/topics/graphics/2d-graphics.html#nine-patch">here</a>
and <a target="_blank" href="http://developer.android.com/tools/help/draw9patch.html">there</a>. (Read these first). These docs though are meant for developers
and lack information for those who work with already compiled 3rd party applications. There you can find information how to create them, but
no information about how they actually work.
<br /><br />
I will try and explain it here. The official docs mess one point that 9patch images come in two forms: source & compiled.
<ul>
  <li><strong>source</strong> - You know this one. You find it in the source of an application or freely available online. These
  are images with a black border around them.</li>
  <li><strong>compiled</strong> - The mysterious form found in apk files. There are no borders and the 9patch data is written into
  a binary chunk called <code>npTc</code>. You can't see or modify it easily, but Android OS can as its quicker to read.</li>
</ul>