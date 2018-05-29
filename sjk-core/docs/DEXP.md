`dexp` command
==============

SJK's dump format may include arbutrary data records besides thread sampling information.
This ablity could be exposed via API.

`dexp` command can be used to export arbitrary data from binary dump format into text or csv format.

Usage
-----

	> java -jar sjk.jar --help dexp
	[Dump Export] Extract metrics form compressed dump into tabular format
	Usage: dexp [options]
	  Options:
		-cl, --columns
		   List of columns (tags) to be exported
		   Default: []
			--commands
		   
		   Default: false
			--explain
		   Include additional information into std out
		   Default: false
			--export-all
		   Export all columns
		   Default: false
	  * -f, --file
		   Input files
		   Default: []
			--help
		   
		   Default: false
		-o, --outfile
		   Out data into a file instead of std out
			--parsers-info
		   Print parsers available in classpath
		   Default: false
			--tags
		   Output statistics for tags
		   Default: false
		-tr, --time-range
		   Time range filter
		-tz, --timezone
		   Set time zone to be used for date formating
		-X, --verbose
		   Enable detailed diagnostics
		   Default: false
		-csv
		   Format output as CSV
		   Default: false

