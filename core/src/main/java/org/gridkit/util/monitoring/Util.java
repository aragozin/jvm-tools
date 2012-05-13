// Copyright (c) 2012 Cloudera, Inc. All rights reserved.
package org.gridkit.util.monitoring;

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

public class Util {
    static void addToolsToClasspath() {
        try {
            String javaHome = System.getProperty("java.home");
            String toolsJarURL = "file:" + javaHome + "/../lib/tools.jar";
            new URL(toolsJarURL).getContent();
            // Make addURL public
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            method.invoke(sysloader, (Object) new URL(toolsJarURL));
            VirtualMachine.class.toString();
        } catch (Exception e) {
            System.err.println("Failed to add tools.jar to classpath: " + e.toString());
            System.exit(1);
        }
    }
    
    public static VirtualMachine attachToPid(String pid) throws Exception {
        System.out.println("Attaching: " + pid);                        
        VirtualMachine jvm = VirtualMachine.attach(pid);
        return jvm;
    }
    
    public static MBeanServerConnection attachJmx(VirtualMachine jvm) throws Exception {
        String addr = attachManagementAgent(jvm);
        System.out.println("JVM JMX uri: " + addr);                     
        JMXServiceURL jmxurl = new JMXServiceURL(addr);
        JMXConnector conn = JMXConnectorFactory.connect(jmxurl);
        MBeanServerConnection mserver = conn.getMBeanServerConnection();
        return mserver;
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
