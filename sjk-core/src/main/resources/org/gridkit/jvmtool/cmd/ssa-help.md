Stack Sample Analyzer (SSA)
===========================

Trace filter expression language
--------------------------------

Some commands in SSA can accept expression for filtering
stack traces for some commands.

Simple expression language can be used to express 
sophisticated filtering criteria.

### Frame matching syntax

Frame matchers are basic element of filter, all frames 
in trace are matched against pattern.

- Frame matcher should match start of frame line.
- `*` and `**` wild card can be used. 
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

There are two positional predicates

- last frame followed by (/+)
- last frame not followed by (/!)

Left side of operator should be one 
or more frame matchers (via comma).

Right is filter expression.
Right filter expression will be applied 
only to portion of trace between last frame 
matched by left argument and end of trace. 

### Parenthesis

Round parenthesis () could be used in expression

### Examples

    **.CoyoteAdapter.service+org.jboss.jca.adapters.jdbc,javax.jdbc
    org.hibernate.!**.SessionImpl.autoFlushIfRequired!org.jboss.jca.adapters.jdbc,javax.jdbc
    **.BijectionInterceptor.aroundInvoke,**.SynchronizationInterceptor.aroundInvoke/!**.proceed