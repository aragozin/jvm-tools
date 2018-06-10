Next version
----
...

- Fixed parsing JFR files under JDK 9/10

0.10.1 - 2018 Jun 4
----
- Java 9/10 compatibility fixes

0.10 - 2018 May 28
----
- Added contention monitoring option to `ttop` command
- `jstack` thread dump text format is now accepted by analytic commands
- Added 'flame' command producing flame chart in HTML format

0.9.3 - 2018 Mar 31
----
- Few minor changes in heap parser

0.9.2 - 2018 Feb 18
----
- Frame histogram can be sorted by terminal count (default sort is by occurance count)
- Export of tabular MBean attributes to CSV
- Fixed NPE in thread dump collector

0.9.1 - 2018 Jan 9
----
- Fixed few issues with hprof heap parser

0.9 - 2017 Dec 24
----
- Fixed undesired splitting of JMX call arguments by comma
- Fixed time range filtering for `stcpy` command
- Added `mprx` command, open JMX port for target process

0.8.1 - 2017 Nov 12
----
- Added SYS cpu time summary to `ssa --thread-info` command
- Few improvements for `dexp` command

0.8 - 2017 Aug 6
----
- Fixed transitive dependencies issues for JFR and NPS parsers
- Fixed HeapPath bug for single asterisk path

0.7 - 2017 Jul 23
----
- Parsing stack traces from NetBeans / VisualVM profiler snapshot format
- Added command to export tags/counter from dump file to CSV or ASCII table

0.6 - 2017 Jun 4
----
- Avoid unnecessary memory allocation to initialize hashCode on `StackFrame` object
- Unified input processing between SSA and STCPY (SSA got wildcard support, STCPY time range filtering and trace trimming)
- Parsing stack traces from Java Flight Recorder files

0.5.1 - 2017 Mar 19
---
- Minor improvements to ThreadDumpSampler
- Added memory MBean sampler

0.5 - 2017 Jan 29
----
- Thread dump archive reader performance improvement
- New generic event reader and writer API
- Binary format V4 with support for non thread dump events
- Added JMX GC event subscriber

0.4.4 - 2016 Dec 4
----
- Compressed heap dump processing majot performance improvement
- Added `SingletonDetector` utility class

0.4.3 - 2016 Aug 18
----
- Added notation for null arguments for MBean operations
- Added few formating options for `mx` command

0.4.2 - 2016 July 7
----
- Option to override JMX URI (support for WildFly JMX over HTTP)
- Fixed locale dependent decimal separator in flame graph generator

0.4.1 - 2016 March 27
----
- Added `--thread-info` mode for SSA command
- Added time range filter option for SSA command
- Added thread name filter option for SSA command

0.4.0 - 2016 Jan 24
----
- Added flame graph generation support (SVG format).
- `--trace-trim` option for SSA commands, command used 
to remove unintersting top of call tree from analysis.

0.3.9 - 2016 Jan 14
----
- Added stack trace filter expression language.
- CLI related stuff is moved into separate module for reuse.
- Fixed potential devision by zero in safe point rate calculation.
- Support of basic primitive wrappers in mx command.
- Optional CSV output for stack trace histogram and categorization.
- Added compressed (gzip) heap dump support.
- Ignore incomplete instances in heap dump.

0.3.8 - 2015 Sep 10
----
- Added safe point information for [ttop] command.
- Fixed NPE in heap cluster analyzer.

0.3.7 - 2015 July 17
----
- Added --dead-young options to [hh] command.
- `org.gridkit.jvmtool.stacktrace.StackTraceCodec.ChainedStackTraceReader` made public.
- Suppress IO exception for parsing stack dump files.

 [hh]: sjk-core/COMMANDS.md#hh-command
 [ttop]: sjk-core/COMMANDS.md#ttop-command