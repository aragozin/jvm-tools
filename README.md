Swiss Java Knife
=========


SJK is a command line tool for JVM diagnostic, troubleshooting and profiling.

Starting sjk
----

java -jar sjk.jar <arguments>

java -jar sjk.jar --help

Below a few command

ttop
----

Pools thread CPU usage of target JVM and periodically report to console.

 - Could sort thread by CPU usage and/or thread's name.
 - Could limit number of thread displayed

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

mxdump
-----

Dumps all MBeans of target java process to JSON.
