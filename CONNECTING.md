Connecting to the target Virtual Machine
========================================

Background
----------

Connection is made to the target VM using sockets, this requires that the
host is running jmx-remote service. Such service can be secured using 
ssl, password, or left unsecured. For a simple illustration, these notes will
show how to configure your target VM for an unsecured connection (but note
this is obviously entirely inappropriate for any real system!)

Building the Swiss Java Knife
-----------------------------

This section presents a very minimal how-to-build since it seems likely that
anyone wanting to use a tool of this power in a Java environment will already
be familiar with how to build projects of this kind. The description assumes
a Unix-like machine working with a command line.

First, ensure you have a JDK available, along with maven and a git client.

Cloned the full project onto your system:

```bash
  git clone https://github.com/aragozin/jvm-tools
```

(Or you might fork the project into your own git repository and clone that. 
This would be more appropriate if you think you might make useful changes
and want to share them back to the world at large)

Change to the project's directory and build with maven:

```bash
  cd jvm-tools
  mvn install
```

If you find that the tests fail, it's still likely possible to build the 
project successfully simply by disabling the tests:

```bash
  mvn install -DskipTests
```

At this point, you'll have several jar files spread around the resulting
directory structure, they can be found using this command. 

```bash
  find . -name \*.jar
```

These files will have maven's typical output names, which include version
numbers and probably the word SNAPSHOT. For example:

```bash
  ./sjk-plus/target/sjk-plus-0.11-SNAPSHOT.jar
```

You could link a shorter name to one of these if you like, but the rest of
these notes will refer to them in place.

Starting the target VM
----------------------

To start the target VM (that is, the VM running the process to be monitored)
add three VM parameters. These are the elements that start -Dcom.sun.management
in the sample commandline below. These configure an unsecured management service
in the target VM, listening on port 9999. Since port 9999 is the default for the
Swiss Java Knife (SJK) tools, this should work directly.

This is the example replace the classpath (`-cp target/allocator.jar`) wnd the main
class designation (`main.Main`) with the appropriate classpath and main class for 
what you want to test.

```bash
java -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -cp target/allocator.jar main.Main
```

Connecting the Swiss Java Knife
-------------------------------

With the target VM running, you're almost ready to connect the client, but first, 
you'll need the process ID of that target VM. This can most easily be obtained
using the java tool "jps". Issue the command:

```bash
jps -l
```

You should see output that bears some resemblance to this:

```bash
22929 jdk.jcmd/sun.tools.jps.Jps
22403 main.Main
10987 org.jetbrains.idea.maven.server.RemoteMavenServer
10796 com.intellij.idea.Main
```

The numbers at the left are process ids, the elements to the right are the main class
that the JVM launched.

Identify which of these processes is the one you want to connect to and make a note
of the number at the left; the PID.

Now you can run the SJK program for the command of your choice. To collect information
on the GC events of the main.Main command in the list above, you would issue this
command:

```bash
java -jar ./sjk-plus/target/sjk-plus-0.11-SNAPSHOT.jar gc --pid 22403
```

Full details of running the JMX remote protocol in a secured way are available
from Oracle's documentation pages at: https://docs.oracle.com/javadb/10.10.1.2/adminguide/radminjmxenabledisable.html

