`ttop` command
==============

ttop commands displays list of JVM threads with CPU usage and memory allocation rate details

Usage
-----

    > java -jar sjk.jar ttop -help
    
    Usage: ttop [options]
      Options:
        -f, --filter
           Wild card expression to filter threads by name
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
-------

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
