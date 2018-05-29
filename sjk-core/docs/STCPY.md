`stcpy` command
===============

This command allow manipulation of thread dump file content. E.g. lumping multiple files to one or filtering content.

Command accepts all supported formats but output is always SJK binary dump file.

Supported input formats are

 - native SJK super comressed binary format produced by [`stcap`](STCAP.md) command
 - VisualVM thread sampler snapshot files
 - Java Flight Recorder recordings
 - `jstack` thread dumps


Usage
-----
	
	> java -jar sjk.jar --help stcpy
	[Stack Copy] Stack dump copy/filtering utility
	Usage: stcpy [options]
	  Options:
			--commands
		   
		   Default: false
		-e, --empty
		   Retain threads without stack trace in dump (ignored by default)
		   Default: false
			--help
		   
		   Default: false
	  * -i, --input
		   Input files
		   Default: []
			--mask
		   One or more masking rules. E.g. com.mycompany:com.somecomplany
		   Default: []
	  * -o, --output
		   Name of file to write thread dump
			--parsers-info
		   Print parsers available in classpath
		   Default: false
		-ss, --subsample
		   If below 1.0 some frames will be randomly throwen away. E.g. 0.1 - every
		   10th will be retained
		   Default: 1.0
		-tn, --thread-name
		   Thread name filter (Java RegEx syntax)
		-tr, --time-range
		   Time range filter
		-tz, --time-zone
		   Time zone used for timestamps and time ranges
		   Default: UTC
		-tf, --trace-filter
		   Apply filter to traces before processing. Use --ssa-help for more details
		   about filter notation
		-tt, --trace-trim
		   Positional filter trim frames to process. Use --ssa-help for more details
		   about filter notation
		-X, --verbose
		   Enable detailed diagnostics
		   Default: false


