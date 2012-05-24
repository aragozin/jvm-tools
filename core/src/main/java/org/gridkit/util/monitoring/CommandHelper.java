package org.gridkit.util.monitoring;

import java.util.List;

public class CommandHelper {

	public static void execMBeanCommand(MBeanCommand cmd, List<String> args) {
		try {
			String jxc = args.get(0);
		}
		catch(IndexOutOfBoundsException e) {
			System.err.println("JVM reference is missing");
			throw new 
		}
	}	
}
