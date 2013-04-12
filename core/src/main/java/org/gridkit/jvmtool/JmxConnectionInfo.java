package org.gridkit.jvmtool;

import javax.management.MBeanServerConnection;

import org.gridkit.lab.jvm.attach.AttachManager;

import com.beust.jcommander.Parameter;

public class JmxConnectionInfo {

	@Parameter(names = {"-p", "--pid"}, description = "JVM process PID")
	private Long pid;
	
	@Parameter(names = {"-s", "--socket"}, description = "Socket address for JMX port")
	private String sockAddr; 

	public MBeanServerConnection getMServer() {
		if (pid == null && sockAddr == null) {
			SJK.failAndPrintUsage("JVM porcess is not specified");
		}
		
		if (pid != null && sockAddr != null) {
			SJK.failAndPrintUsage("You can specify eigther PID or JMX socket connection");
		}

		if (pid != null) {
			return AttachManager.getDetails(pid).getMBeans();
		}
		else {
			throw new UnsupportedOperationException();
		}		
	}
}
