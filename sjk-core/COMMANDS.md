Command of sjk-core module
=========

`ttop` command
----
ttop commands displays list of JVM threads with CPU usage and memory allocation rate details

Usage

    > java -jar sjk.jar ttop -help
    
    Usage: ttop [options]
      Options:
        -f, --filter
           Wild card expression to filter thread by name
            --help
           
           Default: false
        -o, --order
           Sort order. Value tags: CPU, USER, SYS, ALLOC, NAME
            --password
           Password for JMX authentication (only for socket connection)
        -p, --pid
           JVM process PID
        -ri, --report-interval
           Interval between CPU usage reports
           Default: 10000
        -si, --sampler-interval
           Interval between polling MBeans
           Default: 50
        -s, --socket
           Socket address for JMX port (host:port)
        -n, --top-number
           Number of threads to show
           Default: 2147483647
            --user
           User for JMX authentication (only for socket connection)

Example

    > java -jar sjk.jar ttop -p 6344 -n 20 -o CPU

    2013-09-09T11:32:45.426+0300 Process summary
      process cpu=31.08%
      application cpu=28.90% (user=6.40% sys=22.49%)
      other: cpu=2.19%
      heap allocation rate 5260kb/s
    [000001] user= 3.12% sys=11.40% alloc=  762kb/s - main
    [092018] user= 0.94% sys= 0.47% alloc=  335kb/s - RMI TCP Connection(16)-10.139.211.172
    [092016] user= 0.31% sys= 1.56% alloc= 1927kb/s - SVN-WJGGZ
    [092007] user= 0.78% sys= 8.75% alloc=  860kb/s - Worker-4863
    [092012] user= 0.31% sys= 0.31% alloc=  429kb/s - Worker-4864
    [091966] user= 0.16% sys= 0.00% alloc=   90kb/s - Worker-4859
    [092022] user= 0.16% sys= 0.00% alloc=  6871b/s - JMX server connection timeout 92022
    [000002] user= 0.00% sys= 0.00% alloc=     0b/s - Reference Handler
    [000003] user= 0.00% sys= 0.00% alloc=     0b/s - Finalizer
    [000004] user= 0.00% sys= 0.00% alloc=     0b/s - Signal Dispatcher
    [000005] user= 0.00% sys= 0.00% alloc=     0b/s - Attach Listener
    [000009] user= 0.00% sys= 0.00% alloc=     0b/s - Framework Active Thread
    [000012] user= 0.00% sys= 0.00% alloc=     0b/s - Framework Event Dispatcher
    [000014] user= 0.00% sys= 0.00% alloc=     0b/s - Start Level Event Dispatcher
    [000015] user= 0.00% sys= 0.00% alloc=     0b/s - Bundle File Closer
    [000018] user= 0.00% sys= 0.00% alloc=     0b/s - [Timer] - Main Queue Handler
    [000019] user= 0.00% sys= 0.00% alloc=     0b/s - Worker-JM
    [000029] user= 0.00% sys= 0.00% alloc=     0b/s - [ThreadPool Manager] - Idle Thread
    [000030] user= 0.00% sys= 0.00% alloc=     0b/s - Java indexing
    [000033] user= 0.00% sys= 0.00% alloc=     0b/s - com.google.inject.internal.util.$Finalizer

`jps` command
----
List local JVM processes in greater details.

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


`hh` command
----
Extended version of `jmap -histo` command.

    > java -jar sjk.jar --help hh
    Usage: hh [options]
      Options:
            --dead
           Dead objects histogram
           Default: false
            --help
    
           Default: false
            --live
           Live objects histogram
           Default: false
        -p, --pid
           Process ID
           Default: 0
        -n, --top-number
           Show only N top buckets
           Default: 2147483647

`gc` command
----
Reports GC events from target JVM.

    > java -jar sjk.jar --help gc
    Usage: gc [options]
      Options:
            --help
    
           Default: false
        -p, --pid
           JVM process PID
        -s, --socket
           Socket address for JMX port (host:port)

`mx` command
----
MX command allows CLI access to MBean attributes and operations.

    > java -jar sjk.jar --help mx
    Usage: mx [options]
      Options:
        -all, --allMatched
           Process all matched MBeans
           Default: false
        -a, --arguments
           Arguments for MBean operation invocation
           Default: []
        -f, --field, --attribute
           MBean attribute
      * -b, --bean
           MBean name
        -mc, --call
           Invokes MBean method
           Default: false
        -mg, --get
           Retrieves value of MBean attribute
           Default: false
            --help

           Default: false
        -mi, --info
           Display metadata for MBean
           Default: false
        -op, --operation
           MBean operation name to be called
            --password
           Password for JMX authentication (only for socket connection)
        -p, --pid
           JVM process PID
        -ms, --set
           Sets value for MBean attribute
           Default: false
        -s, --socket
           Socket address for JMX port (host:port)
            --user
           User for JMX authentication (only for socket connection)
        -v, --value
           Value to set to attribute

This command have 4 sub commands: `-mi`, `-mg`, `-ms`, `-mc` - exactly one of them could be used at time.

#### Display MBean meta info `-mi`

Example:

    > java -jar sjk.jar mx -p 6344 -mi -b java.lang:type=Threading

    java.lang:type=Threading
    sun.management.ThreadImpl
     - Information on the management interface of the MBean
     (A) ThreadAllocatedMemoryEnabled : boolean - WRITEABLE
     (A) ThreadAllocatedMemorySupported : boolean
     (A) ThreadCount : int
     (A) CurrentThreadCpuTime : long
     (A) CurrentThreadCpuTimeSupported : boolean
     (A) ThreadContentionMonitoringEnabled : boolean - WRITEABLE
     (A) DaemonThreadCount : int
     (A) PeakThreadCount : int
     (A) ObjectMonitorUsageSupported : boolean
     (A) SynchronizerUsageSupported : boolean
     (A) ThreadContentionMonitoringSupported : boolean
     (A) ThreadCpuTimeEnabled : boolean - WRITEABLE
     (A) AllThreadIds : long[]
     (A) CurrentThreadUserTime : long
     (A) TotalStartedThreadCount : long
     (A) ThreadCpuTimeSupported : boolean
     (O) getThreadAllocatedBytes(long p0) : long
     (O) getThreadAllocatedBytes(long[] p0) : long[]
     (O) getThreadCpuTime(long[] p0) : long[]
     (O) getThreadCpuTime(long p0) : long
     (O) getThreadUserTime(long[] p0) : long[]
     (O) getThreadUserTime(long p0) : long
     (O) getThreadInfo(long[] p0, int p1) : CompositeData[]
     (O) getThreadInfo(long[] p0, boolean p1, boolean p2) : CompositeData[]
     (O) getThreadInfo(long p0, int p1) : CompositeData
     (O) getThreadInfo(long[] p0) : CompositeData[]
     (O) getThreadInfo(long p0) : CompositeData
     (O) dumpAllThreads(boolean p0, boolean p1) : CompositeData[]
     (O) findDeadlockedThreads() : long[]
     (O) findMonitorDeadlockedThreads() : long[]
     (O) resetPeakThreadCount() : void

#### Retrive MBean attribute `-mg`

Example

    > java -jar sjk.jar mx -p 6344 -mg -b java.lang:type=Memory -f HeapMemoryUsage

    committed: 419037184
    init:      134217728
    max:       419037184
    used:      356566800

#### Update MBean attribute `-ms`

Example

    > java -jar sjk.jar mx -p 6344 -ms -b java.lang:type=Threading -f ThreadContentionMonitoringEnabled -v true

#### Invoke MBean operation `-mc`

    > java -jar sjk.jar mx -p 6344 -mc -b java.lang:type=Threading -op dumpAllThreads -a false false
    
    blockedCount|blockedTime|inNative|lockInfo                                |lockName                                |lockOwnerId|lockOwnerName|lockedMonitors|lockedSynchronizers|stackTrace                              |suspended|threadId|threadName                              |threadState  |waitedCount|waitedTime
    ------------+-----------+--------+----------------------------------------+----------------------------------------+-----------+-------------+--------------+-------------------+----------------------------------------+---------+--------+----------------------------------------+-------------+-----------+----------
    2           |0          |false   |{className=[I,identityHashCode=17460180}|[I@10a6bd4                              |-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87816   |JMX server connection timeout 87816     |TIMED_WAITING|3          |64        
    3           |0          |false   |{className=[I,identityHashCode=19740445}|[I@12d371d                              |-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87810   |JMX server connection timeout 87810     |TIMED_WAITING|4          |42312     
    2           |0          |false   |{className=[I,identityHashCode=24995862}|[I@17d6816                              |-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87809   |JMX server connection timeout 87809     |TIMED_WAITING|3          |59808     
    3           |0          |false   |{className=[I,identityHashCode=3979158} |[I@3cb796                               |-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87808   |JMX server connection timeout 87808     |TIMED_WAITING|4          |70314     
    0           |0          |false   |null                                    |null                                    |-1         |null         |              |                   |{className=sun.management.ThreadImpl,...|false    |87807   |RMI TCP Connection(13)-10.139.211.172   |RUNNABLE     |4          |70040     
    0           |0          |false   |{className=org.eclipse.jface.text.rec...|org.eclipse.jface.text.reconciler.Dir...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87794   |org.eclipse.jdt.internal.ui.text.Java...|TIMED_WAITING|371        |187629    
    417         |59         |false   |{className=org.eclipse.core.internal....|org.eclipse.core.internal.jobs.Worker...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87536   |Worker-4474                             |TIMED_WAITING|322        |274933    
    597         |50         |false   |{className=org.eclipse.core.internal....|org.eclipse.core.internal.jobs.Worker...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87477   |Worker-4473                             |TIMED_WAITING|484        |317778    
    477         |53         |false   |{className=org.eclipse.core.internal....|org.eclipse.core.internal.jobs.Worker...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87442   |Worker-4469                             |TIMED_WAITING|567        |328929    
    0           |0          |false   |{className=java.util.concurrent.locks...|java.util.concurrent.locks.AbstractQu...|-1         |null         |              |                   |{className=sun.misc.Unsafe,fileName=U...|false    |87417   |RMI Scheduler(0)                        |TIMED_WAITING|25         |587696    
    0           |0          |true    |null                                    |null                                    |-1         |null         |              |                   |{className=java.net.PlainSocketImpl,f...|false    |87415   |RMI TCP Accept-0                        |RUNNABLE     |0          |0         
    2566        |70         |false   |{className=org.eclipse.core.internal....|org.eclipse.core.internal.jobs.Worker...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |87339   |Worker-4463                             |TIMED_WAITING|3554       |624468    
    0           |0          |false   |{className=java.util.Collections$Sync...|java.util.Collections$SynchronizedRan...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |86671   |org.eclipse.wst.sse.ui.internal.recon...|TIMED_WAITING|16434      |632622    
    0           |0          |false   |{className=java.util.Collections$Sync...|java.util.Collections$SynchronizedRan...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |86670   |org.eclipse.wst.sse.ui.internal.recon...|TIMED_WAITING|16435      |632621    
    0           |0          |false   |{className=java.util.Collections$Sync...|java.util.Collections$SynchronizedRan...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |85508   |org.eclipse.wst.sse.ui.internal.recon...|TIMED_WAITING|21430      |632623    
    0           |0          |false   |{className=org.eclipse.jface.text.rec...|org.eclipse.jface.text.reconciler.Dir...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |84797   |org.eclipse.jdt.internal.ui.text.Java...|TIMED_WAITING|23241      |632649    
    86          |2          |false   |{className=org.eclipse.jface.text.rec...|org.eclipse.jface.text.reconciler.Dir...|-1         |null         |              |                   |{className=java.lang.Object,fileName=...|false    |84712   |org.eclipse.jdt.internal.ui.text.Java...|TIMED_WAITING|23460      |631606    
    ...
