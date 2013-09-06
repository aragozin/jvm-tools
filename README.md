Swiss Java Knife
=========

SJK is a command line tool for JVM diagnostic, troubleshooting and profiling.


Prebuild binaries (2013-09-06) are below, though I would encourge you to build jars from sources.
- [sjk-plus.jar - all commands](https://github.com/aragozin/jvm-tools/blob/downloads/sjk-plus-0.1-2013-09-06.jar?raw=true)
- [sjk.jar - all commands without mxdump](https://github.com/aragozin/jvm-tools/blob/downloads/sjk-0.1-2013-09-06.jar?raw=true)


Starting sjk
----

    java -jar sjk.jar <cmd> <arguments>
    java -jar sjk.jar --help
    java -jar sjk.jar --help <cmd>

Below a few command

ttop
----

Pools thread CPU usage of target JVM and periodically report to console.

 - Could sort thread by CPU usage and/or thread's name.
 - Could limit number of thread displayed
 - Display thread memory allocation rate and cumulative process allocation rate (if supported by JVM)

jps
----

Similar to jps. 

- Plus could filter process java processes by their system properties.
- Plus could display specific system properties of process in output.
- Plus could display values of specific -XX for HotSpot JVM processes. 

hh
----

Similar to jmap -histo.

- Plus can show histogram of dead objects (histograms of all and live requested, then difference is caluclated).
- Plus can show N top buckets in histogram.

gcrep
-----

Report information about GC in real time. Data is retrieved via JMX.

mx
-----

This command allow you to do basic operations with MBean from command line.

It can
- read MBean attributes
- update MBean writeable attributes
- invoke MBean operations (arguments are supported)
- displays composite and tabular data in human readable format
- use wild cards to shorten MBean names (e.g. `*:*,name=CodeCacheManager` instead of `java.lang:type=MemoryManager,name=CodeCacheManager`)
- connect to local JVM processes by PID (e.i. any Java process, you do not need to enable JMX server)
- connect to JMX using host:port (password authentication is supported)

[More details](https://github.com/aragozin/jvm-tools/blob/master/sjk-core/COMMANDS.md#mx-command)

mxdump
-----

Dumps all MBeans of target java process to JSON.
