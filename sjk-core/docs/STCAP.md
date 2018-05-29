`stcap` command
===============

stcap commands dumps threads from target process with configured period (or non-stop)

Usage
-----

    [Stack Capture] Dumps stack traces to file for further processing
    Usage: stcap [options]
      Options:
        -e, --empty
           Retain threads without stack trace in dump (ignored by default)
           Default: false
        -f, --filter
           Wild card expression to filter thread by name
           Default: .*
            --help
           
           Default: false
        -l, --limit
           Target number of traces to collect, once reached command will terminate
           (0 - unlimited)
           Default: 0
        -m, --match-frame
           Frame filter, only traces containing this string will be included in dump
      * -o, --output
           Name of file to write thread dump to
            --password
           Password for JMX authentication (only for socket connection)
        -p, --pid
           JVM process PID
        -r, --rotate
           If specified output file would be rotated every N traces (0 - do not
           rotate)
           Default: 0
        -i, --sampler-interval
           Interval between polling MBeans
           Default: 0
        -s, --socket
           Socket address for JMX port (host:port)
        -t, --timeout
           Time until command terminate even without enough traces collected
           Default: 30000
            --user
           User for JMX authentication (only for socket connection)
        -X, --verbose
           Enable detailed diagnostics
           Default: false

Example
-------

Sample target JVM process for 30 seconds

    > java -jar sjk.jar stcap -p PID -o dump.std -t 30s
