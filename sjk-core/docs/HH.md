`hh` command
============

Extended version of `jmap -histo` command. 

**Warning:** Heap histogram requires stop the world pause and some options require full GC for target process.

Usage
-----

    > java -jar sjk.jar --help hh
    Usage: hh [options]
      Options:
           Default: false
            --dead
           Dead objects histogram
           Default: false
            --dead-young
           Histogram for sample of dead young objects
           Default: false
            --help

           Default: false
            --live
           Live objects histogram
           Default: false
        -p, --pid
           Process ID
           Default: 0
        -d, --sample-depth
           Used with --dead-young option. Specific time duration to collect young
           population.
           Default: 10000
        -n, --top-number
           Show only N top buckets
           Default: 2147483647
        -X, --verbose
           Enable detailed diagnostics
           Default: false

`--dead` option make two subsequent histograms (first for all objects then live only) and calculates difference.

`--dead-young` options forces full GC then wait for specified period of time (default 10 seconds) then produce `--dead` histogram. 

Idea is following: after full GC there are no garbage in heap. So `--dead` histogram is showing only objects recenctly allocated (young garbage).
There are two problems with that aprouch though. First, finalizer may prevent some objects to be collected in first full GC. 
Second, if young GC is performed between full GC and `--dead` histogram sampling, garbage population can be skewed.
Command tracks young GC counter and warns if young GC was detected.
