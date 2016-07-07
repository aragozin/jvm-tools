/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.lab.jvm.attach.AttachManager;

import com.beust.jcommander.Parameter;

/**
 * Configurable connection for JMX based commands.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JmxConnectionInfo {

    private CommandLauncher commandHost;
    
	@Parameter(names = {"-p", "--pid"}, description = "JVM process PID")
	private Long pid;
	
	@Parameter(names = {"-s", "--socket"}, description = "Socket address for JMX port (host:port)")
	private String sockAddr; 
	
	@Parameter(names = {"--user"}, description="User for JMX authentication (only for socket connection)")
	private String user = null;

	@Parameter(names = {"--password"}, description="Password for JMX authentication (only for socket connection)")
	private String password = null;

	public JmxConnectionInfo(CommandLauncher host) {
        this.commandHost = host;
    }
	
	public Long getPID() {
	    return pid;
	}
	
	public MBeanServerConnection getMServer() {
		if (pid == null && sockAddr == null) {
			commandHost.failAndPrintUsage("JVM process is not specified");
		}
		
		if (pid != null && sockAddr != null) {
		    commandHost.failAndPrintUsage("You can specify eigther PID or JMX socket connection");
		}

		if (pid != null) {
			MBeanServerConnection mserver = AttachManager.getDetails(pid).getMBeans();
			if (mserver == null) {
			    commandHost.fail("Failed to access MBean server: " + pid);
			}
            return mserver;
		}
		else if (sockAddr != null) {
			String host = host(sockAddr);
			int port = port(sockAddr);
			Map<String, Object> env = null;
			if (user != null || password != null) {
				if (user == null || password == null) {
				    commandHost.failAndPrintUsage("Both user and password required for authentication");
				}
				env = Collections.singletonMap(JMXConnector.CREDENTIALS, (Object)new String[]{user, password});
			}
			MBeanServerConnection mserver = connectJmx(host, port, env);
            if (mserver == null) {
                commandHost.fail("Failed to access MBean server: " + host + ":" + port);
            }
            return mserver;
		}
		else {
			throw new UnsupportedOperationException();
		}		
	}

	@SuppressWarnings("resource")
	private MBeanServerConnection connectJmx(String host, int port, Map<String, Object> props) {
		try {
                        String proto = System.getProperty("jmx.service.protocol", "rmi");

                        final String uri = proto == "rmi" ?
                          "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi" :
                          "service:jmx:" + proto + "://" + host + ":" + port;
                  
			JMXServiceURL jmxurl = new JMXServiceURL(uri);						
			JMXConnector conn = props == null ? JMXConnectorFactory.connect(jmxurl) : JMXConnectorFactory.connect(jmxurl, props);
			// TODO credentials
			MBeanServerConnection mserver = conn.getMBeanServerConnection();
			return mserver;
		} catch (MalformedURLException e) {
		    commandHost.fail("JMX Connection failed: " + e.toString(), e);
		} catch (IOException e) {
		    commandHost.fail("JMX Connection failed: " + e.toString(), e);
		}
		return null;
	}

	private String host(String sockAddr) {
		int c = sockAddr.indexOf(':');
		if (c <= 0) {
		    commandHost.fail("Invalid socket address: " + sockAddr);
		}
		return sockAddr.substring(0, c);
	}

	private int port(String sockAddr) {
		int c = sockAddr.indexOf(':');
		if (c <= 0) {
		    commandHost.fail("Invalid socket address: " + sockAddr);
		}
		try {
			return Integer.valueOf(sockAddr.substring(c + 1));
		} catch (NumberFormatException e) {
		    commandHost.fail("Invalid socket address: " + sockAddr);
			return 0;
		}
	}
}
