`mprx` command
==============

`mprx` command opens JMX port on target JVM (specified by PID or socket address) suitable for remote 
connections from normal JMX based tools, such as JVisualVM or Java Mission Control.

It could be also used to solve problem with improprly setup remote JMX end point using `--force-address` option.

SJK acts as proxy in this case and socket would be closed after termination of SJK process.

Usage
-----

	> java -jar sjk.jar mprx --help
	JMX proxy - expose target process' MBeans for remote access
	Usage: mprx [options]
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
	  * -b
		   Bind address - HOST:PORT

Example of process side proxy
-----------------------------

You can run `mprx` command on the same host as target process. While JMX agent could be started in runtime using `jcmd` from JDK,
sometimes JMX remote connectivy requires overriding `java.rmi.server.hostname` JVM system propery which cannot be done without
restart.

With `mprx` you can run JMX end point as separate process with full control overconfiguration.

Example of client side proxy
----------------------------

Even if remote JMX port is open and exposed, JMX connetion from tools such as VisualVM and MissionControl may not pass through.
Issue, usually, is in JMX handshake providing unroutable network address for data connection. You can use `mxping` to verify that.

SJK has `--force-address` to solve that kind of issue. If you want to use other tools, e.g. VisualVM, you can proxy JMX though SJK.

	> java -jar sjk.jar mprx -s server.local:7777 --force-address -b 127.0.0.1:7777

Assuming server.local:7777 is listening JMX connections, you can not point VisualVM to 127.0.0.1:7777 and access remote MBean tree.

This also can be usefull if you are using any kind of port forwarding for remote access.
