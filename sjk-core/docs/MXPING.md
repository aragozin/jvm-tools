`mxping` command
==============

`mxping` command can be used to troubleshoot JMX connectivity. It accepts eigther process PID 
or JMX socket address. In both cases process is throughfully logged helping problem cause identification.

In case of remote connection in sophysticated network topology, connection address received from RMI registry
on first step of JMX handshake may be incrorrect. `--force-address` option would be useful here to ignore address
from RMI stub.

Usage
-----

	> java -jar sjk.jar mxping --help
	[MXPING] Verify JMX connection to target JVM
	Usage: mxping [options]
	  Options:
			--commands
		   
		   Default: false
			--force-address
		   Override hostname:port for JMX RMI stub, default false
		   Default: false
			--force-hostname
		   Override hostname for JMX RMI stub, default false
		   Default: false
			--force-port
		   Override port for JMX RMI stub, default false
		   Default: false
			--help
		   
		   Default: false
			--password
		   Password for JMX authentication (only for socket connection)
		-p, --pid
		   JVM process PID
		-s, --socket
		   Socket address for JMX port (host:port)
			--user
		   User for JMX authentication (only for socket connection)
		-X, --verbose
		   Enable detailed diagnostics
		   Default: false

Output example
--------------

	> java -jar sjk.jar mxping -s 127.0.0.1:34000
	SJK is running on: OpenJDK 64-Bit Server VM 25.275-b01 (BellSoft)
	Java home: C:\WarZone\WarPath\Java\jdk1.8.0_275+1_bellsoft_x64\jre
	Try to connect via TLS
	Establishing connection to 127.0.0.1:34000
	Failed to connect using TLS: java.rmi.ConnectIOException: error during JRMP connection establishment; nested exception is: 
		javax.net.ssl.SSLHandshakeException: Remote host terminated the handshake
	Try to use plain socket
	Establishing connection to 127.0.0.1:34000
	Establishing connection to 192.168.100.1:34000
	Remote VM: OpenJDK 64-Bit Server VM 25.275-b01 (BellSoft)

As you can see in the listing above actual connection, as a part of JMX handshake, is made to 192.168.100.1:34000. 
That address is provided in RMI remote stub received during first step of JXM handshake.

If address provided by JXM host is not routeable `--force-address` option could solve connectivity issue.