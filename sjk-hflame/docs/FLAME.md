`flame` command description
===========================

`flame` command accepts thread sampling data and produces [flame graph visualization][fg_ui] in form of interactive HTML file.

Command example
---------------

    java -jar sjk.jar flame -f dump.skj -o flame.html

Options description
-------------------

	>java -jar sjk.jar flame --help
	Generates flame graph from stack traces
	Usage: flame [options]
	  Options:
			--commands

		   Default: false
	  * -f, --file
		   Input files
		   Default: []
			--help

		   Default: false
	  * -o, --output
		   Name of generated report file
			--parsers-info
		   Print parsers available in classpath
		   Default: false
		-tn, --thread-name
		   Thread name filter (Java RegEx syntax)
		-tr, --time-range
		   Time range filter
		-tz, --timezone, --time-zone
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

		   
 [fg_ui]: flame_graph_ui.md