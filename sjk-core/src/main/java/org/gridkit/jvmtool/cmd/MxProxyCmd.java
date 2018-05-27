/**
 * Copyright 2017 Alexey Ragozin
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
package org.gridkit.jvmtool.cmd;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class MxProxyCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "mprx";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new MxProxy(host);
    }

    @Parameters(commandDescription = "JMX proxy - expose target process' MBeans for remote access")
    public static class MxProxy implements Runnable {
        
        @ParametersDelegate
        private CommandLauncher host;
        
        @ParametersDelegate
        private JmxConnectionInfo jmxConnectionInfo;
        
        @Parameter(names = {"-b"}, required = true, description = "Bind address - HOST:PORT")
        String bindAddress = null;
        
        public MxProxy(CommandLauncher host) {
            this.host = host;
            this.jmxConnectionInfo = new JmxConnectionInfo(host);
        }

        @Override
        public void run() {
            try {
            	
            	String bhost = "0.0.0.0";
            	int bport = 0;
            	
            	if (bindAddress != null) {
            		int c = bindAddress.indexOf(':');
            		if (c < 0) {            			
            			bport = Integer.valueOf(bindAddress);
            		}
            		else {
            			String h = bindAddress.substring(0, c);
            			String p = bindAddress.substring(c + 1);
            			bhost = h;            		
            			bport = Integer.valueOf(p);
            		}            		
            	}
            	
            	if (bport <= 0) {
            		host.fail("Valid bind port required");
            		return;
            	}
            	
        		MBeanServerConnection connection = jmxConnectionInfo.getMServer();
        		System.out.println("Connected to target JMX endpoint");
            	
        		startLocalJMXEndPoint(bhost, bport, connection);
        		System.out.println("JMX proxy is running - " + bhost + ":" + bport);
        		System.out.println("Interrupt this command to kill proxy");
        		// running until interrupted
        		while(true) {
        			Thread.sleep(1000);
        		}            	
            	
            } catch (Exception e) {
                host.fail(e.toString(), e);
            }
        }
    }
    
    private static void startLocalJMXEndPoint(String bindhost, int port, MBeanServerConnection conn) throws MalformedURLException, IOException {
        
        String host = bindhost == null ? "localhost" : bindhost;
        InetAddress localInetAddress = null;
        try {
            localInetAddress = InetAddress.getByName(host);
            host = localInetAddress.getHostAddress();
        } catch (UnknownHostException localUnknownHostException) {
        }

        if ((localInetAddress == null) || (localInetAddress.isLoopbackAddress())) {
            host = "127.0.0.1";
        }

        String uri = "service:jmx:rmi://" + host + ":" + port + "/jmxrmi";
        
        System.out.println("Open proxy JMX end point on URI - " + uri);
        
        ProxyMBeanServer proxyM = new ProxyMBeanServer(conn);
        
        RMIJRMPServerImpl serverImpl = new RMIJRMPServerImpl(port, null, null, new HashMap<String, Object>());
        RMIConnectorServer server = new RMIConnectorServer(new JMXServiceURL(uri), new HashMap<String, Object>(), serverImpl, proxyM);
        server.start();
        
        new SingleEntryRegistry(port, "jmxrmi", serverImpl.toStub());
    }
    
    @SuppressWarnings({ "serial", "restriction" })
    static class SingleEntryRegistry extends sun.rmi.registry.RegistryImpl {
        private final String name;
        private final Remote object;

        SingleEntryRegistry(int port, String name, Remote stub) throws RemoteException {
            super(port);
            this.name = name;
            this.object = stub;
        }

        SingleEntryRegistry(int port, RMIClientSocketFactory clientSocketFactory, RMIServerSocketFactory serverSocketFactory, String name, Remote stub) throws RemoteException {
            super(port, clientSocketFactory, serverSocketFactory);
            this.name = name;
            this.object = stub;
        }

        public String[] list() {
            return new String[] { this.name };
        }

        public Remote lookup(String paramString) throws NotBoundException {
            if (paramString.equals(this.name))
                return this.object;
            throw new NotBoundException("Not bound: \"" + paramString + "\" (only " + "bound name is \"" + this.name + "\")");
        }

        public void bind(String paramString, Remote paramRemote) throws AccessException {
            throw new AccessException("Cannot modify this registry");
        }

        public void rebind(String paramString, Remote paramRemote) throws AccessException {
            throw new AccessException("Cannot modify this registry");
        }

        public void unbind(String paramString) throws AccessException {
            throw new AccessException("Cannot modify this registry");
        }
    }
    
    public static class ProxyMBeanServer implements MBeanServer {
    	
    	private MBeanServerConnection delegate;
		private SingeClRepo clRepo = new SingeClRepo(this.getClass().getClassLoader());
    	
    	public ProxyMBeanServer(MBeanServerConnection backRegistry) {
    		this.delegate = backRegistry;
    	}

		public ObjectInstance createMBean(String className, ObjectName name) {
			throw new UnsupportedOperationException(); 
		}

		public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) {
			throw new UnsupportedOperationException(); 
		}

		public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) {
			throw new UnsupportedOperationException(); 
		}

		public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) {
			throw new UnsupportedOperationException();
		}

		public void unregisterMBean(ObjectName name) {
			throw new UnsupportedOperationException();
		}

		public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
			try {
				return delegate.getObjectInstance(name);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
			try {
				return delegate.queryMBeans(name, query);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
			try {
				return delegate.queryNames(name, query);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean isRegistered(ObjectName name) {
			try {
				return delegate.isRegistered(name);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public Integer getMBeanCount() {
			try {
				return delegate.getMBeanCount();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
			try {
				return delegate.getAttribute(name, attribute);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
			try {
				return delegate.getAttributes(name, attributes);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
			try {
				delegate.setAttribute(name, attribute);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
			try {
				return delegate.setAttributes(name, attributes);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
			try {
				return delegate.invoke(name, operationName, params, signature);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public String getDefaultDomain() {
			try {
				return delegate.getDefaultDomain();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public String[] getDomains() {
			try {
				return delegate.getDomains();
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
			try {
				delegate.addNotificationListener(name, listener, filter, handback);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
			try {
				delegate.addNotificationListener(name, listener, filter, handback);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
			try {
				delegate.removeNotificationListener(name, listener);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
			try {
				delegate.removeNotificationListener(name, listener, filter, handback);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
			try {
				delegate.removeNotificationListener(name, listener);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void removeNotificationListener(ObjectName name, NotificationListener listener,	NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
			try {
				delegate.removeNotificationListener(name, listener, filter, handback);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public MBeanInfo getMBeanInfo(ObjectName name)	throws InstanceNotFoundException, IntrospectionException, ReflectionException {
			try {
				return delegate.getMBeanInfo(name);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
			try {
				return delegate.isInstanceOf(name, className);
			}
			catch(IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object instantiate(String className) throws ReflectionException, MBeanException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object instantiate(String className, ObjectName loaderName)	throws ReflectionException, MBeanException, InstanceNotFoundException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectInputStream deserialize(ObjectName name, byte[] data)	throws InstanceNotFoundException, OperationsException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data)	throws InstanceNotFoundException, OperationsException, ReflectionException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
			return this.getClass().getClassLoader();
		}

		@Override
		public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
			return this.getClass().getClassLoader();
		}

		@Override
		public ClassLoaderRepository getClassLoaderRepository() {
			return clRepo;
		}
    }    
    
    private static class SingeClRepo implements ClassLoaderRepository {

    	private final ClassLoader loader;
    	
		public SingeClRepo(ClassLoader loader) {
			this.loader = loader;
		}

		@Override
		public Class<?> loadClass(String className) throws ClassNotFoundException {
			return loader.loadClass(className);
		}

		@Override
		public Class<?> loadClassWithout(ClassLoader exclude, String className) throws ClassNotFoundException {
			return loader.loadClass(className);
		}

		@Override
		public Class<?> loadClassBefore(ClassLoader stop, String className) throws ClassNotFoundException {
			return loader.loadClass(className);
		}
    }
}
