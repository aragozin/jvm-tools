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
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteRef;
import java.util.Collections;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;
import javax.management.remote.rmi.RMIServerImpl_Stub;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessDetails;

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

    @Parameter(names = {"--force-address"}, description="Override hostname:port for JMX RMI stub, default false")
    private boolean forceAddress = false;

    @Parameter(names = {"--force-hostname"}, description="Override hostname for JMX RMI stub, default false")
    private boolean forceHostAddress = false;

    @Parameter(names = {"--force-port"}, description="Override port for JMX RMI stub, default false")
    private boolean forcePort = false;

    private boolean diagMode = false;

    public JmxConnectionInfo(CommandLauncher host) {
        this.commandHost = host;
    }

    public void setDiagMode(boolean diagMode) {
        this.diagMode = diagMode;
    }

    public Long getPID() {
        return pid;
    }

    private String formatFailedMsg(Object vm) {
        return "Failed to access MBean server: " + String.valueOf(pid) + "\nFor information about troubleshooting visit\nhttps://github.com/aragozin/jvm-tools/blob/master/sjk-core/docs/TROUBLESHOOTING.md";
    }

    public MBeanServerConnection getMServer() {

        if (forceAddress) {
            forceHostAddress = true;
            forcePort = true;
        }

        if (pid == null && sockAddr == null) {
            commandHost.failAndPrintUsage("JVM process is not specified");
        }

        if (pid != null && sockAddr != null) {
            commandHost.failAndPrintUsage("You can specify eigther PID or JMX socket connection");
        }

        if (pid != null) {
            if (diagMode) {
                System.out.println("Attaching to process " + pid);
            }
            JavaProcessDetails jpd = AttachManager.getDetails(pid);
            if (diagMode) {
                try {
                    jpd.getVmFlag("MaxHeapSize");
                }
                catch(Exception e) {
                    commandHost.fail("Failed to send command via JVM attach channel", e);
                }
            }
            MBeanServerConnection mserver = jpd.getMBeans();
            if (mserver == null) {
                if (diagMode) {
                    try {
                        String uri = (String)jpd.getAgentProperties().get("com.sun.management.jmxremote.localConnectorAddress");
                        if (uri == null || uri.trim().length() == 0) {
                            System.out.println("Failed to start local MBean server on remote VM");
                        }
                        else {
                            System.out.println("Local MBean server URI: " + uri);
                        }
                    } catch (Exception e) {
                        System.out.println("Faield to read agent properties on remote VM");
                        e.printStackTrace(System.out);
                    }
                }
                commandHost.fail(formatFailedMsg(pid));
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
                commandHost.fail(formatFailedMsg(host + ":" + port));
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
            RMIServer rmiServer = null;
            try {
                if (diagMode) {
                    System.out.println("Try to connect via TLS");
                }
                Registry registry = LocateRegistry.getRegistry(host, port, new EnforcedSocketFactory(host, port, new SslRMIClientSocketFactory()));
                try {
                    rmiServer = (RMIServer) registry.lookup("jmxrmi");
                    rmiServer = overrideClientSocketFactory(host, port, rmiServer);
                } catch (NotBoundException nbe) {
                    throw (IOException)
                            new IOException(nbe.getMessage()).initCause(nbe);
                }
            } catch (IOException e) {
                if (diagMode) {
                    System.out.println("Failed to connect using TLS: " + e.toString());
                    System.out.println("Try to use plain socket");
                }
                Registry registry = LocateRegistry.getRegistry(host, port, new EnforcedSocketFactory(host, port, new DirectSocketFactory()));
                try {
                    rmiServer = (RMIServer) registry.lookup("jmxrmi");
                    rmiServer = overrideClientSocketFactory(host, port, rmiServer);
                } catch (NotBoundException nbe) {
                    if (diagMode) {
                        System.out.println("Failed using LocateRegistry. Fallback to JMXConnectorFactory");
                    }
                }
            }
            if(rmiServer != null) {
                RMIConnector rmiConnector = new RMIConnector(rmiServer, props);
                rmiConnector.connect();
                return rmiConnector.getMBeanServerConnection();
            }

            String proto = System.getProperty("jmx.service.protocol", "rmi");

            final String uri = "rmi".equals(proto) ?
                    "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi" :
                    "service:jmx:" + proto + "://" + host + ":" + port;

            if (diagMode) {
                System.out.println("Using JMX URI: " + uri);
            }

            JMXServiceURL jmxurl = new JMXServiceURL(uri);

            JMXConnector conn = props == null ? JMXConnectorFactory.connect(jmxurl) : JMXConnectorFactory.connect(jmxurl, props);
            MBeanServerConnection mserver = conn.getMBeanServerConnection();
            return mserver;

        } catch (MalformedURLException e) {
            commandHost.fail("JMX Connection failed: " + e.toString(), e);
        } catch (IOException e) {
            commandHost.fail("JMX Connection failed: " + e.toString(), e);
        }
        return null;
    }

    @SuppressWarnings("restriction")
    private RMIServer overrideClientSocketFactory(String hostname, int port, RMIServer rmiServer) {
        try {
            if (rmiServer instanceof RemoteObject) {
                RemoteRef ref = ((RemoteObject)rmiServer).getRef();
                sun.rmi.transport.LiveRef lref = ((sun.rmi.server.UnicastRef)ref).getLiveRef();
                ObjID oid = lref.getObjID();
                sun.rmi.transport.tcp.TCPEndpoint ep = (sun.rmi.transport.tcp.TCPEndpoint)(lref.getChannel().getEndpoint());

                sun.rmi.transport.LiveRef nlref = new sun.rmi.transport.LiveRef(oid,
                            new sun.rmi.transport.tcp.TCPEndpoint(ep.getHost(), ep.getPort(), new EnforcedSocketFactory(hostname, port, ep.getClientSocketFactory()), null),
                            false);

                return new RMIServerImpl_Stub(new sun.rmi.server.UnicastRef2(nlref));
            }
        } catch (Exception e) {
            if (diagMode) {
                System.out.println("Failed to inject socket factory into RMIServer stub: " + e.toString());
            }
        }
        return rmiServer;
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

    private class EnforcedSocketFactory implements RMIClientSocketFactory {

        private final RMIClientSocketFactory delegate;
        private final String hostname;
        private final int port;

        public EnforcedSocketFactory(String hostname, int port, RMIClientSocketFactory delegate) {
            this.delegate = delegate != null ? delegate : new DirectSocketFactory();
            this.hostname = hostname;
            this.port = port;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            String origAddr = host + ":" + port;
            String chost = forceHostAddress ? hostname : host;
            int cport = forcePort ? this.port : port;
            String connAddr = chost + ":" + cport;
            if (diagMode) {
                if (!origAddr.equals(connAddr)) {
                    System.out.println("Establishing connection to " + chost + ":" + cport + " (overriden from " + origAddr + ")");
                } else {
                    System.out.println("Establishing connection to " + chost + ":" + cport);
                }
            }
            return delegate.createSocket(chost, cport);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private class DirectSocketFactory implements RMIClientSocketFactory {

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return new Socket(host, port);
        }

        @Override
        public String toString() {
            return "DirectSocketFactory";
        }
    }
}
