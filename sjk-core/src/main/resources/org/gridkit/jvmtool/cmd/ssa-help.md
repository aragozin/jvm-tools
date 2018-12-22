Stack Sample Analyzer (SSA)
===========================

SSA has a number of subcommands. Every command is a specific
type of report based on a population of stack traces 
from a dump file.

Available subcommands are

- `--print` Outputs traces in plain text
- `--histo` Produces frame frequency histogram
- `--flame` Produces "flame graph" in SVG format
- `--thread-info` Report aggregates of trace dumps 
                  segregated by threads

Input population of traces can be filtered in various ways

- `-tf` Filter traces using predicate expression
- `-tt` Transforms (trim trace root) based on expression
- `-tn` Filter by tread name (Java RegEx syntax)
- `-tr` Filter by time range

Trace filter expression language
--------------------------------

Some commands in SSA can accept expression for filtering
stack traces for some commands.

Stack trimming is another option using same expression
location to identify a trim point within each trace.

The simple expression language can be used to express 
sophisticated filtering criteria.

### Frame matching syntax

Frame matchers are basic element of filter, all frames 
in the trace are matched against the pattern.

- Frame matcher should match the start of frame line.
- `*` and `**` wildcard can be used. 
  `**` match anything, while `*` will match anything except `.`
  
Examples

`javax.jdbc` - match anything in `javax.jdbc` package

`**.MyClass` - match any frame containing `.MyClass`

`java.util.ResourceBundle.getObject*:395` - this example include line number matching

### Operators

- comma (,) is used as OR operator
- plus (+) is used as AND operator
- exclamation (!) is equivalent to AND NOT

OR have priority over AND/AND NOT
AND and AND NOT are left associative

### Positional predicates

There are four positional predicates

- last frame followed by (/+)
- last frame not followed by (/!)
- first frame followed by (/^+)
- first frame not followed by (/^!)

Left-side of operator MUST be a frame matcher
(or multiple via comma operator).

Right is filter expression.
Right filter expression will be applied 
only to a portion of trace between the last frame 
matched by left argument and end of trace. 

### Parenthesis

Round parenthesis () could be used in an expression

### Trimming

Trimming expression should be either positional operator
or conjunction of frame matchers.

If conjunction of frame matchers is used as trimming expression trimming point would be the first frame matched.

### Examples

    **.CoyoteAdapter.service+org.jboss.jca.adapters.jdbc,javax.jdbc
    org.hibernate.!**.SessionImpl.autoFlushIfRequired!org.jboss.jca.adapters.jdbc,javax.jdbc
    **.BijectionInterceptor.aroundInvoke,**.SynchronizationInterceptor.aroundInvoke/!**.proceed

Time range filtering
--------------------------------

All times are displayed/parsed in UTC time zone by default.
Desired time zone could be specified via `-tz` option.

Date range consists of two dates separated by `-` (dash).

`YYYY.MM.dd_HH:mm:ss` format should be used.

Leftmost parts of data can be omitted (though both dates should be of the same length).

### Examples

    2016.02.21_03:01:30-2016.02.22_04:01:30
    02.21_03:01:30-02.22_04:01:30
    21_03:01:30-22_04:01:30
    03:01:30-04:01:30
    05:30-10:30
