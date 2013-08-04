Young GC benchmark command
=========


This commands performs young bench mark in started JVM.

General benchmarking approach
----

Benchmark is simulating uniform write-heavy memory usage
pattern.

In general, application is doing put and removes from big sharded hash table.
Number of possible keys is capped (to limit a population of live objects).

java.util.HashMap is used for each shard. Individual size of each shard
is capped at 200k entries. HaspMap table is grows by usual rules, thus tables
should be fairly randomly distributed in old space.

Size of population to be used in test is calculated dynamically based
of available old space size in JVM.

JVM heap initially populated with desired number of entries, then measurement is started.

You can set desired test termination criteria using

- Wallclock time
- Number of young collections
- Number of old collections  


Mark Sweep Compact mode
----

If MSC algorithm is detected, population size will be calculated to fill
whole old space - HEAD ROOM (256MiB by default).



Concurrent GC mode
----

If CMS or G1 algorithm is detected, population size will be calculated to fill half
of available old space.

After initial population is created, benchmark will shuffle memory some more time to
ensure that live data are spread across whole memory space.

DRY mode
----

Dry mode is enabled by command line option. Dry mode initializes heap as usual,
but once test begging, no more puts or gets will happen (though data generation will be done as usual).

This option were introduced to study card marking write barrier. As no data is written to old space 
and no data survive in young space, pause time will be dominated by card scanning time.

Data modes
----

Various data could be used to fill hash table.

- STRING - java.lang.String of fixed length will be used as values.
- LONG - new instance of java.lang.Long will be created for value. 
- INT - new instance java.lang.Integer will be created as value.
- CONST - single object will be used as value.
  
Default mode is STRING with length 64


Rationale
------

Few goals were influencing design of benchmark

- it should adapt itself for different memory sizes, producing comparable results
- operations should be simple and uniform and avoid statistical noise in result
- it was designed with card marking write barrier in mind 