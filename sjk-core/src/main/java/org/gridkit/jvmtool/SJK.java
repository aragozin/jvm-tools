package org.gridkit.jvmtool;

import java.util.Arrays;
import java.util.List;

import org.gridkit.jvmtool.cli.CommandLauncher;

public class SJK extends CommandLauncher {

    public static void main(String[] args) {
        new SJK().start(args);
    }    
    
	protected List<String> getCommandPackages() {
	    return Arrays.asList("org.gridkit.jvmtool.cmd", "org.gridkit.jvmtool.hflame.cmd");
	}
}
