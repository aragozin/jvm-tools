Swiss Java Knife
=========

SJK is a command line tool for JVM diagnostic, troubleshooting and profiling.


Prebuild binaries 
[ ![Download](https://api.bintray.com/packages/aragozin/generic/sjk/images/download.svg) ](https://bintray.com/aragozin/generic/sjk/_latestVersion) are below, though I would encourge you to build jars from sources.
- [sjk-plus.jar - all commands](https://bintray.com/artifact/download/aragozin/generic/sjk/2014-09-06/sjk-plus.jar)
- [sjk.jar - all commands without mxdump](https://bintray.com/artifact/download/aragozin/generic/sjk/2014-09-06/sjk.jar)


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

[More details](sjk-core/COMMANDS.md#ttop-command)

jps
----

Similar to jps. 

- Plus could filter process java processes by their system properties.
- Plus could display specific system properties of process in output.
- Plus could display values of specific -XX for HotSpot JVM processes. 
 
[More details](sjk-core/COMMANDS.md#jps-command)

hh
----

Similar to jmap -histo.

- Plus can show histogram of dead objects (histograms of all and live requested, then difference is caluclated).
- Plus can show N top buckets in histogram.

[More details](sjk-core/COMMANDS.md#hh-command)

gc
-----

Report information about GC in real time. Data is retrieved via JMX.

[More details](sjk-core/COMMANDS.md#gc-command)

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

[More details](sjk-core/COMMANDS.md#mx-command)

mxdump
-----

Dumps all MBeans of target java process to JSON.
