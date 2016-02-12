package org.gridkit.jvmtool.bstub;

import java.io.IOException;
import java.util.Random;

import org.gridkit.jvmtool.cli.CommandLauncher;

import com.beust.jcommander.Parameter;

import net.java.btrace.client.Client;

public class BTraceClientConnection {

    private CommandLauncher host;
    
    @Parameter(names = "-p", description = "PID of target process", required = true)
    private int pid = -1;
    
    @Parameter(names = "--bt-debug", description = "Enabled BTrace debug mode", required = false)
    private boolean btraceDebug = false;
    
    public BTraceClientConnection(CommandLauncher host) {
        this.host = host;
    }

    public int getPID() {
        return pid;
    }
    
    public Client connect() throws IOException {

        Client client = Client.forPID(pid);
        if (client == null) {
            host.fail("Failed to connected to " + pid);
        }

        client.setDebug(btraceDebug);
        client.setUnsafe(true);
        client.setPort(50000 + new Random(System.currentTimeMillis()).nextInt(5000));
        client.setAgentPath(AgentHelper.initAgentJar());
        
        //TODO should add tools.jar of target VM
        client.setSysCp(client.getSysCp());

        client.attach();
        
        return client;
    }
}
