package org.gridkit.jvmtool.bstub;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.lab.jvm.perfdata.JStatData;
import org.gridkit.lab.jvm.perfdata.JStatData.Counter;
import org.gridkit.lab.jvm.perfdata.JStatData.StringCounter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import net.java.btrace.client.Client;

@Parameters(commandDescription = "Stub probe to target process")
public class StubCommand implements Runnable {

    @ParametersDelegate
    private final CommandLauncher host;
    
    @ParametersDelegate
    private BTraceClientConnection bconn;
    
    @Parameter(names = {"-s", "--script"}, description = "Script class file", required = true)
    private String script;

    @Parameter(names = {"-a", "--args"}, variableArity = true, description = "Script arguments", required = false)
    private String[] args;
    
    public StubCommand(CommandLauncher host) {
        this.host = host;
        this.bconn = new BTraceClientConnection(host);
    }

    @Override
    public void run() {
        try {
            int pid = bconn.getPID();
            System.out.println("Target JVM " + pid);
            try {
                Map<String, Counter<?>> data = JStatData.connect(pid).getAllCounters();
                String vm_name = ((StringCounter)data.get("java.property.java.vm.name")).getString();
                String vm_java_version = ((StringCounter)data.get("java.property.java.version")).getString();
                String vm_info = ((StringCounter)data.get("java.property.java.vm.info")).getString();
                String vm_version = ((StringCounter)data.get("java.property.java.vm.version")).getString();
                System.out.println(vm_name);
                System.out.println(vm_java_version + "  " + vm_info + "  " + vm_version);
            }
            catch(Exception e) {
                System.out.println("JVM info is not available (" + e.toString() + ")");
            }

            File f = new File(script);
            if (!f.isFile()) {
                host.fail("No such file [" + f.getCanonicalPath() + "]");
            }

            Client client = bconn.connect();
            byte[] data = StreamHelper.readFile(f);
            
            client.submit(f.getName(), data, args);
            
            System.out.println("Press any key to exit");
            System.in.read();
            
        } catch (IOException e) {
            host.fail("", e);
        }
    }
}
