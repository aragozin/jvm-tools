package org.gridkit.jvmtool;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.gridkit.jvmtool.cmd.Command;
import org.gridkit.jvmtool.cmd.HeapInfoCmd;
import org.gridkit.jvmtool.cmd.MBeanCommand;
import org.gridkit.jvmtool.cmd.ProcListCmd;
import org.gridkit.jvmtool.cmd.SimpleCommand;

public class JTool {

	private static Command[] COMMANDS = {
		new ProcListCmd(),
		new HeapInfoCmd(),
	};
	
    public static void main(String[] argsArray) {
    	List<String> args = Arrays.asList(argsArray);
    	try {
	        if (args.size() == 0) {
	            printUsageAndExit();
	        }
	        else if ("help".equals(args.get(0))) {
	        	printHelpAndExit(args.subList(1, args.size()));
	        }
	        else {
	        	for(Command cmd: COMMANDS) {
	        		if (cmd.getCommand().equals(args.get(0))) {
	        			runCommandAndExit(cmd, args.subList(1, args.size()));
	        		}
	        	}
	        }

	        System.err.println("Invalid command line: " + args);
        	printUsageAndExit();
    	}
    	catch(InvalidCommandLineException e) {
    		System.err.println(e.toString());
    		printHelpAndExit(args);
    	}
    	catch(Exception e) {
    		System.err.println(e.toString());
    		System.exit(1);
    	}
    }

    private static void runCommandAndExit(Command cmd, List<String> args) throws InvalidCommandLineException, IOException {
		if (cmd instanceof MBeanCommand) {
			CommandHelper.execMBeanCommand((MBeanCommand) cmd, args);
		}
		else if (cmd instanceof SimpleCommand) {
			((SimpleCommand)cmd).exec(args);
		}
		System.exit(0);
	}

	private static void printHelpAndExit(List<String> args) {
    	for(Command cmd: COMMANDS) {
    		if (cmd.getCommand().equals(args.get(0))) {
		    	System.out.println(cmd.getCommand() + " - " + cmd.getDescription());
		    	System.out.print("<JTOOL> ");
		    	cmd.printUsage(System.out);
		    	cmd.printHelp(System.out);
		    	System.exit(1);
    		}
    	}
    	System.err.println("Unknown command: " + args.get(0));
    	printUsageAndExit();
    }

    static void printUsageAndExit() {
        System.err.println("Usage: ");
        for(Command cmd: COMMANDS) {
        	System.err.println(String.format("    %-8s - %s", cmd.getCommand(), cmd.getDescription()));
        }
        System.exit(1);
    }
}
