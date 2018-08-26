
Problem: SJK fails to connect to target JVM by specified PID
======================================================

Example

    > java -jar sjk.jar ttop -p 1234
    Failed to access MBean server: 1234

Possible issues
---------------

 - Verify java command is pointing to `JDK/bin/java` not `JRE/bin/java` or `JDK/jre/bin/java`. SJK will not work without JDK.
 - SJK and target process should run under same user account.
 - If target process has `-Djava.io.tmpdir=path` start up option, you should add same option to SJK command line.
 - Local firewall rules may be blocking access. JMX is still using TCP sockets behind the scene.
 
Suggested trobleshooting steps
------------------------------

 - Try access same PID with JDK command (e.g. `jstack 1234`)
 - Retry SJK command with `-X` flag enabled that would provide more verbose error information


