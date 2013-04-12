package org.gridkit.jvmtool.cmd;

import java.util.List;

import javax.management.MBeanServerConnection;


public interface MBeanCommand extends Command {

	public void exec(MBeanServerConnection connection, List<String> args);
	
}
