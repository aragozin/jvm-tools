`mx` command
============

MX command allows CLI access to MBean attributes and operations.

Usage
-----

    > java -jar sjk.jar --help mx
	[MBean] MBean query and invokation
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
			--commands
		   
		   Default: false
			--csv
		   Used with --get command, result would be formatted as CSV
		   Default: false
		-mg, --get
		   Retrieves value of MBean attribute
		   Default: false
			--help
		   
		   Default: false
		-mi, --info
		   Display metadata for MBean
		   Default: false
			--max-col-width
		   Table column width threshold for formating tabular data
		   Default: 40
		-op, --operation
		   MBean operation name to be called
			--password
		   Password for JMX authentication (only for socket connection)
		-p, --pid
		   JVM process PID
			--quiet
		   Avoid non-essential output
		   Default: false
		-ms, --set
		   Sets value for MBean attribute
		   Default: false
		-s, --socket
		   Socket address for JMX port (host:port)
			--user
		   User for JMX authentication (only for socket connection)
		-v, --value
		   Value to set to attribute
		-X, --verbose
		   Enable detailed diagnostics
		   Default: false

This command have 4 sub commands: `-mi`, `-mg`, `-ms`, `-mc` - exactly one of them could be used at time.

Display MBean meta info `-mi`
-----------------------------

Example:

    > java -jar sjk.jar mx -p 6344 -mi -b java.lang:type=Threading

    java.lang:type=Threading
    sun.management.ThreadImpl
     - Information on the management interface of the MBean
     (A) ThreadAllocatedMemoryEnabled : boolean - WRITEABLE
     (A) ThreadAllocatedMemorySupported : boolean
     (A) ThreadCount : int
     ...
     (O) getThreadAllocatedBytes(long p0) : long
     (O) getThreadAllocatedBytes(long[] p0) : long[]
     (O) getThreadCpuTime(long[] p0) : long[]
     ...

Retrive MBean attribute `-mg`
-----------------------------

Example

    > java -jar sjk.jar mx -p 6344 -mg -b java.lang:type=Memory -f HeapMemoryUsage

    committed: 419037184
    init:      134217728
    max:       419037184
    used:      356566800

Update MBean attribute `-ms`
----------------------------

Example

    > java -jar sjk.jar mx -p 6344 -ms -b java.lang:type=Threading -f ThreadContentionMonitoringEnabled -v true

Invoke MBean operation `-mc`
----------------------------

Example

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
