`ssa` command
=============

`ssa` command's used to analyze data files conating Java thread sampling information.

Currently supported formats are

 - native SJK super comressed binary format produced by [`stcap`](STCAP.md) command
 - VisualVM thread sampler snapshot files
 - Java Flight Recorder recordings
 - `jstack` thread dumps

Usage
-----

    > java -jar sjk.jar ssa --help
	[Stack Sample Analyzer] Analyzing stack trace dumps
	Usage: ssa [options]
	  Options:
			--by-term
		   Sort frame histogram by terminal count
		   Default: false
			--categorize
		   Print summary for provided categorization
		   Default: false
		-cf, --categorizer-file
		   Path to file with stack trace categorization definition
			--commands
		   
		   Default: false
		-co, --csv-output
		   Output data in CSV format
		   Default: false
		-f, --file
		   Path to stack dump file
			--flame
		   Export flame graph to SVG format
		   Default: false
			--help
		   
		   Default: false
			--histo
		   Print frame histogram
		   Default: false
		-nc, --named-class
		   May be used with some commands to define name stack trace classes
	       Use <name>=<filter expression> notation
		   Default: []
			--parsers-info
		   Print parsers available in classpath
		   Default: false
			--print
		   Print traces from file
		   Default: false
		-rc, --rainbow
		   List of filters for rainbow coloring
			--ssa-help
		   Additional information about SSA
		   Default: false
		-si, --summary-info
		   List of summaries
			--thread-info
		   Per thread info summary
		   Default: false
		-tn, --thread-name
		   Thread name filter (Java RegEx syntax)
		-tr, --time-range
		   Time range filter
		-tz, --time-zone
		   Time zone used for timestamps and time ranges
		   Default: UTC
			--title
		   Flame graph title
		   Default: Flame Graph
		-tf, --trace-filter
		   Apply filter to traces before processing. Use --ssa-help for more details
		   about filter notation
		-tt, --trace-trim
		   Positional filter trim frames to process. Use --ssa-help for more details
		   about filter notation
		-X, --verbose
		   Enable detailed diagnostics
		   Default: false
			--width
		   Flame graph width in pixels
		   Default: 1200

Following subcommands are available:

`--print` print stack trace in text format.

`--histo` produces frame histogram from dump file. Below is example of histogram.

	Trc N     Frm N Term N     Frame                                                                                                                 
	21727 57% 21727 0      0%  java.lang.Thread.run(Thread.java:662)                                                                                 
	16002 42% 16002 16002  42% java.lang.Object.wait(Native Method)                                                                                  
	8923  23% 8923  8923   23% java.net.SocketInputStream.socketRead0(Native Method)                                                                 
	8923  23% 8923  0      0%  java.net.SocketInputStream.read(SocketInputStream.java:129)                                                           
	6402  16% 9603  0      0%  java.lang.reflect.Method.invoke(Method.java:597)                                                                      
	6402  16% 9603  0      0%  sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)                                 
	6399  16% 6399  0      0%  java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:118)                                                          
	5722  15% 5722  0      0%  java.io.BufferedInputStream.fill(BufferedInputStream.java:218)                                                        
	5722  15% 5722  0      0%  java.io.BufferedInputStream.read(BufferedInputStream.java:237)                                                        

- Trc N - number of traces containing frame (percentage from total trace count)
- Frm N - number of occurrences for this frame in all traces (same frame may be on stack for multiple times)
- Term N - number of traces terminating with that frame

`--flame` produces flame graph in SVG format (see also [`flame`][hflame] command).

`--categorize` calculate hit count for each category in provided eigther by classification file or via `-nc` option.

See also [SSA documentation page](../src/main/resources/org/gridkit/jvmtool/cmd/ssa-help.md).

 [hflame]: ../../sjk-hflame/docs/FLAME.md