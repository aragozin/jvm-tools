`jps` command
=============

List local JVM processes in greater details.

Usage
-----
    > java -jar sjk.jar --help jps
    Usage: jps [options]
      Options:
        -fd, --filter-description
           Wild card expression to match process description
        -fp, --filter-property
           Wild card expressions to match JVM system properties
            --help
    
           Default: false
        -pd, --process-details
           Print custom information related to a process. Following tags can be
           used: PID, MAIN, FDQN_MAIN, ARGS, D<sys-prop>, d<sys-prop>, X<jvm-flag>

