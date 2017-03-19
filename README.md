Swiss Java Knife
=========

SJK is a command line tool for JVM diagnostic, troubleshooting and profiling.

SJK exploits standard diagnostic interfaces of JVM (such as JMX, JVM attach and perf counters) and add some more logic on top 
to be useful for common troubleshooting case.


Prebuild binaries are available at [bintray.com](https://bintray.com)
[ ![(download)](https://api.bintray.com/packages/aragozin/generic/sjk/images/download.svg) ](https://bintray.com/aragozin/generic/sjk/_latestVersion) are below. I would encourge you to build jars from sources, though.
- [sjk-plus.jar - all commands](https://bintray.com/artifact/download/aragozin/generic/sjk-plus-0.5.1.jar)
- [sjk.jar - all commands without mxdump](https://bintray.com/artifact/download/aragozin/generic/sjk-0.5.1.jar)


Starting sjk
----

    java -jar sjk.jar <cmd> <arguments>
    java -jar sjk.jar --commands
    java -jar sjk.jar --help <cmd>

Below a few command from SJK ([full command reference](sjk-core/COMMANDS.md)).

[ttop]
----

Pools thread CPU usage of target JVM and periodically report to console.

 - Could sort thread by CPU usage and/or thread's name.
 - Could limit number of thread displayed
 - Display thread memory allocation rate and cumulative process allocation rate (if supported by JVM)

[More details](sjk-core/COMMANDS.md#ttop-command)

[jps]
----

Similar to jps. 

- Plus could filter process java processes by their system properties.
- Plus could display specific system properties of process in output.
- Plus could display values of specific -XX for HotSpot JVM processes. 
 
[More details](sjk-core/COMMANDS.md#jps-command)

[hh]
----

Similar to jmap -histo.

- Plus can show histogram of dead objects (histograms of all and live requested, then difference is calculated).
- Plus can show N top buckets in histogram.

[More details](sjk-core/COMMANDS.md#hh-command)

[gc]
-----

Report information about GC in real time. Data is retrieved via JMX.

[More details](sjk-core/COMMANDS.md#gc-command)

[stcap] and [ssa]
----

These commands provide basic sample profiler capabilities. `stcap` produces hyper-dense stack trace dump 
(about 1000 compression rate compared to text format) and `ssa` provides few basic reports over dump files. 

So far following reports are available
- stack frame histogram with advanced filtering options
- converting dump to text format

Dump file can be also processed programatically.

[mx]
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

 [ttop]: sjk-core/COMMANDS.md#ttop-command
 [jps]: sjk-core/COMMANDS.md#jps-command
 [hh]: sjk-core/COMMANDS.md#hh-command
 [gc]: sjk-core/COMMANDS.md#gc-command
 [mx]: sjk-core/COMMANDS.md#mx-command
 [stcap]: sjk-core/COMMANDS.md#stcap-command
 [ssa]: sjk-core/COMMANDS.md#ssa-command
