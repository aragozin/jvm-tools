package org.gridkit.jvmtool;

import java.io.PrintStream;
import java.util.List;

import javax.management.MBeanServerConnection;

public interface MBeanCommand {

	public String getCommand();
	
	public String getDescription();
	
	public void printUsage(PrintStream out);
	
	public void printHelp(PrintStream out);
	
	public void exec(MBeanServerConnection connection, List<String> args);
	
}
