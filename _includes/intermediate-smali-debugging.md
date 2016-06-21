<span class="label label-danger lb">Warning</span> SmaliDebugging has been marked as deprecated in 2.0.3, and removed in 2.1. Please check <a href="https://github.com/JesusFreke/smali/wiki/smalidea">SmaliIdea</a> for a debugger.</span>
<br /><br />
Apktool makes possible to debug smali code step by step, watch variables, set breakpoints, etc.

<h3><strong>General information</strong></h3>

Generally we need several things to run Java debugging session:
<br />
<ul>
  <li>debugger server (usually Java VM)</li>
  <li>debugger client (usually IDE like IntelliJ, Eclipse or Netbeans)</li>
  <li>client must have sources of debugged application</li>
  <li>server must have binaries compiled with debugging symbols referencing these sources</li>
  <li>sources must be java files with at least package and class definitions, to properly connect them with debugging symbols</li>
</ul>

In our particular situation we have:
<br />
<ul>
  <li>server: Monitor (Previously DDMS), part of Android SDK, standard for debugging Android applications - explained <a href="http://developer.android.com/guide/developing/tools/ddms.html">here</a></li>
  <li>client: any JPDA client - most of decent IDEs have support for this protocol.</li>
  <li>sources: smali code modified by apktool to satisfy above requirements (".java" extension, class declaration, etc.). Apktool modifies them when decoding apk in debug mode.</li>
  <li>binaries: when building apk in debug mode, apktool removes original symbols and adds new, which are referencing smali code (line numbers, registers/variables, etc.)</li>
</ul>
<br />
<blockquote><span class="label label-info lb">Info</span> To successfully run debug sessions, the apk must be <strong>both</strong> decoded and built in debug mode. Decoding with debug decodes the application
 differently to allow the debug rebuild option to inject lines allowing the debugger to identify variables and types.<code>-d / --debug</code></blockquote>

<h3><strong>General instructions</strong></h3>

Above information is enough to debug smali code using apktool, but if you aren't familiar with DDMS and Java debugging, then you probably still don't know how to do it. 
Below are simple instructions for doing it using IntelliJ or Netbeans.
<br />
<ul>
  <li>Decode apk in debug mode: <kbd>$ apktool d -d -o out app.apk</kbd></li>
  <li>Build new apk in debug mode: <kbd>$ apktool b -d out</kbd></li>
  <li>Sign, install and run new apk.</li>
  <li>Follow sub-instructions below depending on IDE.</li>
</ul>
<h4><strong>IntelliJ (Android Studio) instructions</strong></h4>
<br />
<ul>
  <li>In IntelliJ add new Java Module Project selecting the "out" directory as project location and the "smali" subdirectory as content root dir.</li>
  <li>Run Monitor (Android SDK /tools folder), find your application on a list and click it. Note port information in last column - it should be something like "86xx / 8700".</li>
  <li>In IntelliJ: Debug -> Edit Configurations. Since this is a new project, you will have to create a Debugger.</li>
  <li>Create a Remote Debugger, with the settings on "Attach" and setting the Port to 8700 (Or whatever Monitor said). The rest of fields should be ok, click "Ok".</li>
  <li>Start the debugging session. You will see some info in a log and debugging buttons will show up in top panel.</li>
  <li>Set breakpoint. You must select line with some instruction, you can't set breakpoint on lines starting with ".", ":" or "#".</li>
  <li>Trigger some action in application. If you run at breakpoint, then thread should stop and you will be able to debug step by step, watch variables, etc.</li>
</ul>

<h4><strong>Netbeans instructions</strong></h4>
<br />
<ul>
  <li>In Netbeans add new Java Project with Existing Sources, select "out" directory as project root and "smali" subdirectory as sources dir.</li>
  <li>Run DDMS, find your application on a list and click it. Note port information in last column - it should be something like "86xx / 8700".</li>
  <li>In Netbeans: Debug -> Attach Debugger -> select JPDA and set Port to 8700 (or whatever you saw in previous step). Rest of fields should be ok, click "Ok".</li>
  <li>Debugging session should start: you will see some info in a log and debugging buttons will show up in top panel.</li>
  <li>Set breakpoint. You must select line with some instruction, you can't set breakpoint on lines starting with ".", ":" or "#".</li>
  <li>Trigger some action in application. If you run at breakpoint, then thread should stop and you will be able to debug step by step, watch variables, etc.</li>
</ul>

<h3><strong>Limitations/Issues</strong></h3>
<br />
<ul>
  <li>Because IDE doesn't have full sources, it doesn't know about class members and such.</li>
  <li>Variables watching works because most of data could be read from memory (objects in Java know about their types), but if for example, you watch an object and it has some nulled member, then you won't see, what type this member is.</li>
</ul>
