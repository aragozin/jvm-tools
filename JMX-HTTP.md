WildFly application service support proprietary
JMX over HTTP transport.

Below is example shell script to let SJK use JMX over HTTP instead of JMX over RMI transport.
Additional WildFly client jars are required.

	#!/usr/bin/env bash
	set -eu
	LIB_DIR="${WILDFLY_HOME:-/opt/wildfly}/bin/client"
	JMX_PROTO="http-remoting-jmx"

	java \
	  -Djmx.service.protocol=${JMX_PROTO} \
	  -Djava.ext.dirs=${LIB_DIR} \
	  -jar "/opt/jvm-tools/sjk-plus.jar" $@