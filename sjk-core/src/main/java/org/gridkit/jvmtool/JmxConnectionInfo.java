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

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.gridkit.lab.jvm.attach.AttachManager;

import com.beust.jcommander.Parameter;

/**
 * Configurable connection for JMX based commands.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JmxConnectionInfo {

	@Parameter(names = {"-p", "--pid"}, description = "JVM process PID")
	private Long pid;
	
	@Parameter(names = {"-s", "--socket"}, description = "Socket address for JMX port (host:port)")
	private String sockAddr; 

	public MBeanServerConnection getMServer() {
		if (pid == null && sockAddr == null) {
			SJK.failAndPrintUsage("JVM porcess is not specified");
		}
		
		if (pid != null && sockAddr != null) {
			SJK.failAndPrintUsage("You can specify eigther PID or JMX socket connection");
		}

		if (pid != null) {
			return AttachManager.getDetails(pid).getMBeans();
		}
		else if (sockAddr != null) {
			String host = host(sockAddr);
			int port = port(sockAddr);
			
			return connectJmx(host, port);
		}
		else {
			throw new UnsupportedOperationException();
		}		
	}

	private MBeanServerConnection connectJmx(String host, int port) {
		try {
			final String uri = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
			JMXServiceURL jmxurl = new JMXServiceURL(uri);
			JMXConnector conn = JMXConnectorFactory.connect(jmxurl);
			// TODO credentials
			MBeanServerConnection mserver = conn.getMBeanServerConnection();
			return mserver;
		} catch (MalformedURLException e) {
			SJK.fail(e.toString());
		} catch (IOException e) {
			SJK.fail(e.toString());
		}
		return null;
	}

	private String host(String sockAddr) {
		int c = sockAddr.indexOf(':');
		if (c <= 0) {
			SJK.fail("Invalid socket address: " + sockAddr);
		}
		return sockAddr.substring(0, c);
	}

	private int port(String sockAddr) {
		int c = sockAddr.indexOf(':');
		if (c <= 0) {
			SJK.fail("Invalid socket address: " + sockAddr);
		}
		try {
			return Integer.valueOf(sockAddr.substring(c + 1));
		} catch (NumberFormatException e) {
			SJK.fail("Invalid socket address: " + sockAddr);
			return 0;
		}
	}
}
