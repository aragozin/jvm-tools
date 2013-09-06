Command of sjk-core module
=========

mx
----
MX command allows CLI access to MBean attributes and operations.

    >java -jar sjk.jar --help mx
    Usage: mx [options]
      Options:
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

Display MBean meta info `-mi`
-----

Retrive MBean attribute `-mg`
-----

Update MBean attribute `-ms`
-----

Invoke MBean operation `-mc`
-----
