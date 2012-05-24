package org.gridkit.jvmtool;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;

public class GCReporter {

	static {
        try {
			String javaHome = System.getProperty("java.home");
			String toolsJarURL = "file:" + javaHome + "/../lib/tools.jar";

			// Make addURL public
			Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			method.setAccessible(true);
			URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			method.invoke(sysloader, (Object) new URL(toolsJarURL));
		} catch (Exception e) {
			System.err.println("Failed to add tools.jar to classpath: " + e.toString());
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		String pid = args[0];
		
		System.out.println("Attaching: " + pid);			
		VirtualMachine jvm = VirtualMachine.attach(pid);
		String addr = attachManagementAgent(jvm);
		System.out.println("JVM JMX uri: " + addr);			
		JMXServiceURL jmxurl = new JMXServiceURL(addr);
		JMXConnector conn = JMXConnectorFactory.connect(jmxurl);
		MBeanServerConnection mserver = conn.getMBeanServerConnection();
		System.out.println("MBean server connected");
	
		MBeanGCMonitor rmon = new MBeanGCMonitor(mserver);
		MBeanGCMonitor pmon = new MBeanGCMonitor(mserver);
		final MBeanGCMonitor fmon = new MBeanGCMonitor(mserver);
		
		Thread freport = new Thread() {
			@Override
			public void run() {
				System.out.println("\nTotal");
				System.out.println(fmon.calculateStats());
			}					
		};

		Runtime.getRuntime().addShutdownHook(freport);

		long interval = 60000;
		long deadline = System.currentTimeMillis() + interval;
		System.out.println("Collecting GC stats ...");
		while(true) {
			while(System.currentTimeMillis() < deadline) {
				String report = rmon.reportCollection();
				if (report.length() > 0) {
					System.out.println(report);
				}
				Thread.sleep(50);
			}
			deadline += interval;
			System.out.println();
			System.out.println(pmon.calculateStats());
			System.out.println();
			pmon = new MBeanGCMonitor(mserver);
			if (System.in.available() > 0) {
				System.exit(1);
			}
		}
	}

	private static String attachManagementAgent(VirtualMachine vm) throws Exception
	{
     	Properties localProperties = vm.getAgentProperties();
     	if (localProperties.containsKey("com.sun.management.jmxremote.localConnectorAddress")) {
     		return ((String)localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
     	}
		
		String jhome = vm.getSystemProperties().getProperty("java.home");
	    Object localObject = jhome + File.separator + "jre" + File.separator + "lib" + File.separator + "management-agent.jar";
	    File localFile = new File((String)localObject);
	    
	    if (!(localFile.exists())) {
	       localObject = jhome + File.separator + "lib" + File.separator + "management-agent.jar";
	 
	       localFile = new File((String)localObject);
	       if (!(localFile.exists())) {
	    	   throw new IOException("Management agent not found"); 
	       }
	    }
	 
     	localObject = localFile.getCanonicalPath();     	
     	try {
     		vm.loadAgent((String)localObject, "com.sun.management.jmxremote");
     	} catch (Exception e) {
     		throw e;
     	}
 
     	localProperties = vm.getAgentProperties();
     	return ((String)localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
   	}
}
