Next
----
- Added stack trace filter expression language.
- CLI related stuff is moved into separate module for reuse.
- Fixed potential devision by zero in safe point rate calculation.
- Support of basic primitive wrappers in mx command.
- Optional CSV output for stack trace histogram and categorization.

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