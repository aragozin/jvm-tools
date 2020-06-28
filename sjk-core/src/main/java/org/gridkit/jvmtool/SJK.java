package org.gridkit.jvmtool;

import java.util.Arrays;
import java.util.List;

import org.gridkit.jvmtool.cli.CommandLauncher;

public class SJK extends CommandLauncher {

    @SuppressWarnings("unused")
    private static final boolean JAVA_11 = System.getProperty("java.vm.version").startsWith("11.");

    public static void main(String[] args) {
        new SJK().start(args);
    }

    @Override
    protected String[] getModulesUnlockCommand() {
        return new String[] {"java.base/jdk.internal.perf=ALL-UNNAMED", "jdk.attach/sun.tools.attach=ALL-UNNAMED"};
    }

    protected List<String> getCommandPackages() {
        return Arrays.asList("org.gridkit.jvmtool.cmd", "org.gridkit.jvmtool.hflame.cmd");
    }
}
