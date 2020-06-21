JMX Setup Quick Guide
=====================

Using many Java diagnostic tools (e.g. Visual VM, Mission Control or SJK) over the network requires a remote JVM to listen to JMX protocol on TCP socket.
Typically JMX port is enabled via JVM startup options, though it can also be activated ad hoc via `jcmd`.


Configuring JMX via command line
--------------------------------

[Official documentation][1] is useful, but here is a quick summary.

Minimal config for JMX would look like

    java ...
      -Dcom.sun.management.jmxremote.port=5555
      -Dcom.sun.management.jmxremote.authenticate=false 
      -Dcom.sun.management.jmxremote.ssl=false
       ...

This will let you use hostname:5555 as JMX socket address to connect to JVM.

Example above is totally *insecure*, I have warned you.

For securing JMX please consult [docs][1].

Couple extra properties you better add to the real config:

  * `com.sun.management.jmxremote.rmi.port=5555` set is to same port value
if you do not want JVM to open another random port.

  * `java.rmi.server.hostname` see network quirks below.


JMX Network quirks
------------------

JMX is a weird protocol.

Assuming you have JMX socket `hostA:5555`, communications would look like.

    JXM Client                      JMX Server
       |                                |
       connect on hostA:5555 ----------->
       <---return "stub" hostX:portX ---|
       |                                |
       connect on hostX:portX----------->
       <------- start talking JMX ------>

So the client would attempt to connect to a certain `host:port` returned from JMX Server. 
This is mechanics of Java RMI protocol and it is not very friendly to sophisticated network topologies.

This gives us two strategies for JMX address configuration.

JMX on host you can directly connect
------------------------------------

Directly connect host A means you can ping address A and process on host can bind socket on address A 
(i.e. there is no NAT between you and host A).

In this can minimal *unsecured* config would be.

    java ...
      -Dcom.sun.management.jmxremote.port=5555
      -Dcom.sun.management.jmxremote.rmi.port=5555
      -Dcom.sun.management.jmxremote.authenticate=false 
      -Dcom.sun.management.jmxremote.ssl=false
      -Djava.rmi.server.hostname=HostA
       ...

You may omit `java.rmi.server.hostname` if you have a single interface and not using dual (IPv4 / IPv6) stack.


In this case you would probably need security.

JMX via port forwarding
-----------------------

If you access host via port forwarding (`ssh`, `kubectl` etc), you need to
force JVM to use local host for JMX.

In this can minimal config would be.

    java ...
      -Dcom.sun.management.jmxremote.port=5555
      -Dcom.sun.management.jmxremote.rmi.port=5555
      -Dcom.sun.management.jmxremote.authenticate=false 
      -Dcom.sun.management.jmxremote.ssl=false
      -Djava.rmi.server.hostname=127.0.0.1
       ...

Forwarded port must be the same on both sides. Now both connections made by JMX would be
properly forwarded.

In this case you probably already have a layer of security and leaving JMX unsecured could be justified.


Using `JCMD` to start JMX port without JVM restart
--------------------------------------------------

If you have local access to the host where JVM is running, but no JMX configured you can fix it with `jcmd`.

Below is command to open JMX protocol listener on port 5555.

    jcmd PID ManagementAgent.start \
	jmxremote.authenticate=false \
        jmxremote.ssl=false \
        jmxremote.port=5555 

Configuration options here are the same properties as above but with `com.sun.managment` stripped.

All forwarding issues mentioned above are applied too.


 [1]: https://docs.oracle.com/en/java/javase/11/management/monitoring-and-management-using-jmx-technology.html#GUID-805517EC-2D33-4D61-81D8-4D0FA770D1B8
