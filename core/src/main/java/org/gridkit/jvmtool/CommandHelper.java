package org.gridkit.jvmtool;

import java.io.IOException;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.jmx.JmxConnector;

public class CommandHelper {

	public static void runMain(String arg, String[] args) throws Exception {
		String[] nargs = new String[1 + args.length];
		nargs[0] = arg;
		for(int i = 0; i != args.length; ++i) {
			nargs[i + 1] = args[i];
		}
		Tool.main(nargs);
	}
	
	public static void execMBeanCommand(MBeanCommand cmd, List<String> args) throws InvalidCommandLineException, IOException {
		try {
			String jxc = args.get(0);
			MBeanServerConnection mserv;
			mserv = JmxConnector.connection(jxc);
			cmd.exec(mserv, args.subList(1, args.size()));
		}
		catch(IndexOutOfBoundsException e) {
			System.err.println("JVM reference is missing");
			throw new InvalidCommandLineException("Bad command line: " + args.toString());
		}
	}	
}
