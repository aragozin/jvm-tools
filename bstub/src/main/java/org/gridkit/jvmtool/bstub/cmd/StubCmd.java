package org.gridkit.jvmtool.bstub.cmd;

import org.gridkit.jvmtool.bstub.StubCommand;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;

public class StubCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "stub";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new StubCommand(host);
    }
}
