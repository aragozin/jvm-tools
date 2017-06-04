Next version
----

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