Swiss Java Knife (SJK)
=========

SJK is a command line tool for JVM diagnostic, troubleshooting and profiling.

SJK exploits standard diagnostic interfaces of JVM (such as JMX, JVM attach and perf counters) and adds some more logic on top 
useful for common troubleshooting cases. SJK can also be used as a library for building application specific diagnostic tools
or to enhance your code with self monitoring features.

What you can do with SJK?
----

 - [Monitor Java threads in real time][ttop]
 - [Analyze head dynamics with advanced class histograms][hh]
 - [Access MBean attributes and operation from command line][mx]
 - Create [flame graphs][html-flame] and over reports from snapshots created VisualVM, Java Flight Recorder or [SJK's own sampler][stcap]
 
See [full command reference](sjk-core/COMMANDS.md).

Download
----

Latest prebuild binaries [![Last Version](https://img.shields.io/maven-central/v/org.gridkit.jvmtool/sjk.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.gridkit.jvmtool%22)
- `wget https://training.ragozin.info/sjk.jar` -> [sjk.jar](https://search.maven.org/remotecontent?filepath=org/gridkit/jvmtool/sjk-plus/0.22/sjk-plus-0.22.jar) 

Starting sjk
----

    java -jar sjk.jar <cmd> <arguments>
    java -jar sjk.jar --commands
    java -jar sjk.jar --help <cmd>

Below a few command from SJK ([full command reference](sjk-core/COMMANDS.md)).

[ttop]
----

Pools thread CPU usage of the target JVM and periodically reports to the console.

 - can attach via PID or open JMX port
 - displays thread memory allocation rate and cumulative process allocation rate
 - displays safe point time consumption (only if attache via PID)

[More details][ttop]

[hh]
----

Similar to `jmap -histo`.

 - plus can show a histogram of dead objects (histograms of all and live requested, then difference is calculated)
 - plus can show N top buckets in histogram

[More details][hh]

[stcap], [stcpy], [ssa] and [flame]
----

These commands provide basic sample profiler capabilities. `stcap` produces hyper-dense stack trace dump 
(about 1000 compression rate compared to text format) and `ssa` provides few reports over dump files.
`stcpy` can copy data in archives produced by `stcap` (e.g. merging dumps or filtering selected threads).

So far following reports are available:

 - [sophisticated filtering] (time, stack trace, thread name)
 - stack frame histogram with advanced filtering options
 - flame graph visualization (SVG or [interactive HTML][html-flame])
 - per thread summary (CPU usage, memory allocation, etc)
 - converting back to text format

Dump file can be also processed programatically.

[More details][ssa]

[mx]
-----

This command allows you to do basic operations with MBean from command line.

It can:

 - read MBean attributes
 - update MBean writeable attributes
 - invoke MBean operations (arguments are supported)
 - display composite and tabular data in a human readable format
 - use wild cards to shorten MBean names (e.g. `*:*,name=CodeCacheManager` instead of `java.lang:type=MemoryManager,name=CodeCacheManager`)
 - connect to local JVM processes by PID (e.i. any Java process, you do not need to enable JMX server)
 - connect to JMX using host:port (password authentication is supported)

[More details][mx]

[jps]
----

Similar to `jps` from JDK. 

- plus can display specific system properties of process in output
- plus can display values of specific -XX for HotSpot JVM processes 
- plus can filter process java processes by their system properties
 
[More details][jps]

[gc]
-----

Report information about GC in real time. Data is retrieved via JMX.

[More details][gc]

mxdump
-----

Dumps all MBeans of target java process to JSON.

 [ttop]: sjk-core/docs/TTOP.md
 [jps]: sjk-core/docs/JPS.md
 [hh]: sjk-core/docs/HH.md
 [gc]: sjk-core/docs/GC.md
 [mx]: sjk-core/docs/MX.md
 [stcap]: sjk-core/docs/STCAP.md
 [stcpy]: sjk-core/docs/STCPY.md
 [ssa]: sjk-core/docs/SSA.md
 [flame]: sjk-hflame/docs/FLAME.md
 [sophisticated filtering]: sjk-core/src/main/resources/org/gridkit/jvmtool/cmd/ssa-help.md
 [flame graphs]: http://blog.ragozin.info/2016/01/flame-graphs-vs-cold-numbers.html
 [html-flame]: sjk-hflame/docs/flame_graph_ui.md
