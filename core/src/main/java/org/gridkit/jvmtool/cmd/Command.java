package org.gridkit.jvmtool.cmd;

import java.io.PrintStream;

public interface Command {

	public String getCommand();
	
	public String getDescription();
	
	public void printUsage(PrintStream out);
	
	public void printHelp(PrintStream out);
	
}
