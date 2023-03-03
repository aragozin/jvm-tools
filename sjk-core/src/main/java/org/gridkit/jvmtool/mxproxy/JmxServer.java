package org.gridkit.jvmtool.mxproxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
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

public class JmxServer {

    private final MBeanServerConnection mconn;

    private String bindHost = null;
    private int port = 0;

    public JmxServer(MBeanServerConnection mconn) {
        this.mconn = mconn;
    }

    public JmxServer setBindHost(String bindHost) {
        this.bindHost = bindHost;
        return this;
    }

    public JmxServer setPort(int port) {
        this.port = port;
        return this;
    }

    public String getJmxUri() {
        String host = bindHost == null ? "localhost" : bindHost;

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
        return uri;
    }

    public RMIConnectorServer start() throws IOException {

        String uri = getJmxUri();

        ProxyMBeanServer proxyM = new ProxyMBeanServer(mconn);

        RMIJRMPServerImpl serverImpl = new RMIJRMPServerImpl(port, null, null, new HashMap<String, Object>());
        RMIConnectorServer server = new RMIConnectorServer(new JMXServiceURL(uri), new HashMap<String, Object>(), serverImpl, proxyM);
        server.start();

        new SingleEntryRegistry(port, "jmxrmi", serverImpl.toStub());

        return server;
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

        @Override
        public String[] list() {
            return new String[] { this.name };
        }

        @Override
        public Remote lookup(String paramString) throws NotBoundException {
            if (paramString.equals(this.name))
                return this.object;
            throw new NotBoundException("Not bound: \"" + paramString + "\" (only " + "bound name is \"" + this.name + "\")");
        }

        @Override
        public void bind(String paramString, Remote paramRemote) throws AccessException {
            throw new AccessException("Cannot modify this registry");
        }

        @Override
        public void rebind(String paramString, Remote paramRemote) throws AccessException {
            throw new AccessException("Cannot modify this registry");
        }

        @Override
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

        @Override
        public ObjectInstance createMBean(String className, ObjectName name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unregisterMBean(ObjectName name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
            try {
                return delegate.getObjectInstance(name);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
            try {
                return delegate.queryMBeans(name, query);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
            try {
                return delegate.queryNames(name, query);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isRegistered(ObjectName name) {
            try {
                return delegate.isRegistered(name);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Integer getMBeanCount() {
            try {
                return delegate.getMBeanCount();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
            try {
                return delegate.getAttribute(name, attribute);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
            try {
                return delegate.getAttributes(name, attributes);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
            try {
                delegate.setAttribute(name, attribute);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
            try {
                return delegate.setAttributes(name, attributes);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
            try {
                return delegate.invoke(name, operationName, params, signature);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getDefaultDomain() {
            try {
                return delegate.getDefaultDomain();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String[] getDomains() {
            try {
                return delegate.getDomains();
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
            try {
                delegate.addNotificationListener(name, listener, filter, handback);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
            try {
                delegate.addNotificationListener(name, listener, filter, handback);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
            try {
                delegate.removeNotificationListener(name, listener);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
            try {
                delegate.removeNotificationListener(name, listener, filter, handback);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
            try {
                delegate.removeNotificationListener(name, listener);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void removeNotificationListener(ObjectName name, NotificationListener listener,  NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
            try {
                delegate.removeNotificationListener(name, listener, filter, handback);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public MBeanInfo getMBeanInfo(ObjectName name)  throws InstanceNotFoundException, IntrospectionException, ReflectionException {
            try {
                return delegate.getMBeanInfo(name);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
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
        public Object instantiate(String className, ObjectName loaderName)  throws ReflectionException, MBeanException, InstanceNotFoundException {
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
        public ObjectInputStream deserialize(ObjectName name, byte[] data)  throws InstanceNotFoundException, OperationsException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data)  throws InstanceNotFoundException, OperationsException, ReflectionException {
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
