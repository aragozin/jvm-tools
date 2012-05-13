// Copyright (c) 2012 Cloudera, Inc. All rights reserved.
package org.gridkit.util.monitoring;


/**
 * Main class to invoke other tools.
 */
public class Tool {
    
	static {
        AttachUtil.ensureToolsClasspath();
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsageAndExit();
        }
        String cmd = args[0];
        if ("jmx".equals(cmd)) {
            JmxReporter.main(sublist(args, 1));
        } else if ("top".equals(cmd)) {
            ThreadReporter.main(sublist(args, 1));
        } else if ("gc".equals(cmd)) {
            GCReporter.main(sublist(args, 1));
        } else if ("ps".equals(cmd)) {
            new VirtualMachines().run();
        } else if ("stack".equals(cmd)) {
            sun.tools.jstack.JStack.main(sublist(args, 1));
        } else {
            printUsageAndExit();
        }
    }

    private static String[] sublist(String[] args, int start) {
        String[] ret = new String[args.length - start];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = args[start + i];
        }
        return ret;
    }

    static void printUsageAndExit() {
        System.err.println("Usage: ");
        System.err.println("  jmx <pid>");
        System.err.println("  top <pid>");
        System.err.println("  gc <pid>");
        System.err.println("  ps");
        System.err.println("  stack <pid>");
        System.exit(1);
    }

}
