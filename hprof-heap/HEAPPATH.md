HeapPath is a simple notation to locate object instances in heap dump.

HeapPath is used extensively to define graph traversal rules for heap cluster analyzer.
It is also useful to extract meaningfull descriptions for heap dump objects.

Basic constructs
================

Simples path is below
```
myfield1.myfield2.myfield3
```
Another simple path
```
myarrayfield[0].myfield
```
you could also use
```
myarrayfield[*].myfield
```
or even (for 2 dimentional array)
```
myarrayfield[*][*]
```
HeapPath may select multiple instances (similar to XPath).

Field name could also be replaced by wildcard (*).
Path below is valid.
```
myfield1.*.myfield3
```

HeapPath will try to walk all possible paths. Paths wich cannot be traversed will be silently discarded.

Class predicates
===============

Path below would allow only MyClass instances to be selected.
```
[*].value(MyClass)
```

Class predicate could be used anywhere in path.

  - (MyClass) - will match only exact instances of MyClass
  - (+MyClass) - will match instances of MyClass or subclasses
  - (+java.util.List) - will match NOTHING. java.util.List is an interface and interface inheritance information is not retained in heap dump
  - (+MyClassA|+MyClassB) - will match instances or subclasses of eigther class, MyClassA or MyClassB
  - (java.lang.String) - will match only java.lang.String
  - (**.String) - will match any class named String regardless of package
  - (mypackage.*.A) - will match mypackage.ab.A, but not mypackage.a.b.A

Conditional predicates and map access
=====================================

Let's assume what you would like to select specific key in HashMap. HeapPath allow simple predicate usage.

```
myhashmap.table[*][key=description].value
```

Path above relies on internal structure of java.util.HashMap. Predicate key=description will match map entries with field "key" equlas to description 
(comparison is done in string form).

Path above is not correct though. Not all map entries are directly reachable via array.

HeapPath support special map entry interator which would work with java.util.HashMap and subclasses

```
myhashmap?entrySet[key=description].value
```

Path above is right way to deal with HashMap. It will not work with alterative Map implementation (e.g. ConcurrentHashMap).

Predicate have simple format [field=value]. Everithing between "=" and "]" is interpreted as value.

```
myhashmap?entrySet[key=my.property.name].value
myhashmap?entrySet[key=123].value
myhashmap?entrySet[key=null].value
```

All paths above are valid. [key=null] will match both real nulls and "null" string.


Limitations
===========

Spaces are not allowed in HeapPath at the moment.
Special characters in string literal are not allowed.
