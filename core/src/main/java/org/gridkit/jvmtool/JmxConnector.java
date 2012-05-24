package org.gridkit.jvmtool;

import java.io.File;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * Helper class providing JMX connection using either PID or socket address.
 * TODO support for JMX security
 * 
 * @author Alexey Ragozin
 */
public class JmxConnector {

	private static final Pattern SOCKET_ADDERSS = Pattern.compile("([a-zA-Z0-9_.]*)[:](\\d+)");
	
	public static MBeanServerConnection connection(String connector) throws IOException {
		MBeanServerConnection conn;
		conn = connectByPID(connector);
		if (conn != null) {
			return conn;
		}
		conn = connectBySocketAddress(connector);
		if (conn != null) {
			return conn;
		}
		throw new IOException("Cannot connect to " + connector); 
	}

	private static MBeanServerConnection connectByPID(String connector) throws IOException {
		try {
			int pid = Integer.parseInt(connector);

			System.out.println("Attaching: " + pid);
			VirtualMachine jvm = VirtualMachine.attach(String.valueOf(pid));
			String addr = attachManagementAgent(jvm);
			System.out.println("JVM JMX uri: " + addr);
			JMXServiceURL jmxurl = new JMXServiceURL(addr);
			JMXConnector conn = JMXConnectorFactory.connect(jmxurl);
			MBeanServerConnection mserver = conn.getMBeanServerConnection();
			return mserver;
		} catch (NumberFormatException e) {
			// not a PID, try next
			return null;
		} catch (AttachNotSupportedException e) {
			System.err.println("Attach is not supported: " + connector);
			throw new IOException(e);
		} catch (AgentLoadException e) {
			System.err.println("Agent loading failed: " + connector);
			throw new IOException(e);
		} catch (AgentInitializationException e) {
			System.err.println("Agent loading failed: " + connector);
			throw new IOException(e);
		}
	}

	private static MBeanServerConnection connectBySocketAddress(String connector) throws IOException {
		Matcher matcher = SOCKET_ADDERSS.matcher(connector);
		if (matcher.matches()) {
			try {
				String host = matcher.group(1);
				int port = Integer.parseInt(matcher.group(2));
				Registry reg = LocateRegistry.getRegistry(host, port);
				RMIServer server = ((RMIServer)reg.lookup("jmxrmi"));
				RMIConnector conn = new RMIConnector(server, null);
				return conn.getMBeanServerConnection();
			} catch (NumberFormatException e) {
				throw new IOException(e);
			} catch (AccessException e) {
				System.err.println("Cannot connect to " + connector);
				throw new IOException(e);
			} catch (RemoteException e) {
				System.err.println("Remote exception \"" + e.toString() + "\" at " + connector);
				throw new IOException(e);
			} catch (NotBoundException e) {
				System.err.println("Cannot connect to " + connector + ", not bound");
				throw new IOException(e);
			}
		}
		else {
			return null;
		}
	}

	private static String attachManagementAgent(VirtualMachine vm)	throws IOException, AgentLoadException, AgentInitializationException {
		Properties localProperties = vm.getAgentProperties();
		if (localProperties.containsKey("com.sun.management.jmxremote.localConnectorAddress")) {
			return ((String) localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
		}

		String jhome = vm.getSystemProperties().getProperty("java.home");
		Object localObject = jhome + File.separator + "jre" + File.separator
				+ "lib" + File.separator + "management-agent.jar";
		File localFile = new File((String) localObject);

		if (!(localFile.exists())) {
			localObject = jhome + File.separator + "lib" + File.separator
					+ "management-agent.jar";

			localFile = new File((String) localObject);
			if (!(localFile.exists())) {
				throw new IOException("Management agent not found");
			}
		}

		localObject = localFile.getCanonicalPath();
		vm.loadAgent((String) localObject, "com.sun.management.jmxremote");

		localProperties = vm.getAgentProperties();
		return ((String) localProperties.get("com.sun.management.jmxremote.localConnectorAddress"));
	}
}
