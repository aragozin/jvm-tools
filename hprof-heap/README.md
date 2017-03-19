JVM Heap Dump Analysis library
====

This library allows loading and inspecting JVM heap dumps via API.

Implementation is based on NetBeans profiler.

Changes compared to NetBeans profiler libarary
----

Main motivation for this fork was need in analyzing huge heap dumps (150 GiB).
Both NetBeans and Eclipse Memory Analyzer are using disk to store
support indexes for navigation though object graph.

For interaction graphical tool you really need these support structures.

But for batch processing these indexes do not bring much value.

I have modified original code from NetBeans to let it work without additional
on disk structures.

**Improvements**
 
 * Does not create any temporary files on disk to process heap dump
 * Can work directly GZ compressed heap dumps
 * [HeapPath][1] notation

**Limitations**
 
 * Only forward reference traversing

Examples
----

This is libary not a tool. It can be used from Java code to analyze heap dump.
Here few examples

 * [Printing JSF component trees][JSF]
 * [JBoss session content analysis][Session]

Licensing
----
NetBeans profiler code is dualy licensed by CDDL or GPLv2 licenses (see details in file headers).
All modifications on top of NetBeans code are licensed by Apache 2.0.

 [1]: HEAPPATH.md
 [JSF]: src/test/java/org/gridkit/jvmtool/heapdump/example/JsfTreeExample.java
 [Session]: src/test/java/org/gridkit/jvmtool/heapdump/example/JBossServerDumpExample.java