package org.gridkit.jvmtool;

import java.util.List;

import javax.management.MBeanServerConnection;

public interface MBeanCommand {

	public void exec(MBeanServerConnection connection, List<String> args);
	
}
