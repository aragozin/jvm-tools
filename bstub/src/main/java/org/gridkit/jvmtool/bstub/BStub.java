package org.gridkit.jvmtool.bstub;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.lab.jvm.attach.AttachManager;

public class BStub extends CommandLauncher {

    public static void main(String[] args) {
        AttachManager.ensureToolsJar();
        new BStub().start(args);
    }    
}
