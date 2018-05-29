`mprx` command
==============

`mprx` command opens JMX port on target JVM (specified by PID) suitable for remote connections from normal JMX
based tools, such as JVisualVM or Java Mission Control.

SJK acts as proxy in this case and socket would be closed after termination of SJK process.

Usage
-----

	> java -jar skj.jar --help mprx
	JMX proxy - expose target process' MBeans for remote access
	Usage: mprx [options]
	  Options:
			--commands
		   
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
	  * -b
		   Bind address - HOST:PORT

