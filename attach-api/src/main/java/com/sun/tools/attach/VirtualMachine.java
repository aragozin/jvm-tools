package com.sun.tools.attach;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.sun.tools.attach.spi.AttachProvider;


/**
 * Compile time only mock for attach API.
 */
public abstract class VirtualMachine {
    /**
     * Return a list of Java virtual machines.
     *
     * <p> This method returns a list of Java {@link
     * com.sun.tools.attach.VirtualMachineDescriptor} elements.
     * The list is an aggregation of the virtual machine
     * descriptor lists obtained by invoking the {@link
     * com.sun.tools.attach.spi.AttachProvider#listVirtualMachines
     * listVirtualMachines} method of all installed
     * {@link com.sun.tools.attach.spi.AttachProvider attach providers}.
     * If there are no Java virtual machines known to any provider
     * then an empty list is returned.
     *
     * @return  The list of virtual machine descriptors.
     */
    public static List<VirtualMachineDescriptor> list() {
    	throw new UnsupportedOperationException("You should add tools.jar to classpath");
    }

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> This method obtains the list of attach providers by invoking the
     * {@link com.sun.tools.attach.spi.AttachProvider#providers()
     * AttachProvider.providers()} method. It then iterates overs the list
     * and invokes each provider's {@link
     * com.sun.tools.attach.spi.AttachProvider#attachVirtualMachine(java.lang.String)
     * attachVirtualMachine} method in turn. If a provider successfully
     * attaches then the iteration terminates, and the VirtualMachine created
     * by the provider that successfully attached is returned by this method.
     * If the <code>attachVirtualMachine</code> method of all providers throws
     * {@link com.sun.tools.attach.AttachNotSupportedException AttachNotSupportedException}
     * then this method also throws <code>AttachNotSupportedException</code>.
     * This means that <code>AttachNotSupportedException</code> is thrown when
     * the identifier provided to this method is invalid, or the identifier
     * corresponds to a Java virtual machine that does not exist, or none
     * of the providers can attach to it. This exception is also thrown if
     * {@link com.sun.tools.attach.spi.AttachProvider#providers()
     * AttachProvider.providers()} returns an empty list. </p>
     *
     * @param   id
     *          The abstract identifier that identifies the Java virtual machine.
     *
     * @return  A VirtualMachine representing the target VM.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          <tt>("attachVirtualMachine")</tt>, or another permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the <code>attachVirtualmachine</code> method of all installed
     *          providers throws <code>AttachNotSupportedException</code>, or
     *          there aren't any providers installed.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>id</code> is <code>null</code>.
     */
    public static VirtualMachine attach(String id) throws AttachNotSupportedException, IOException {
    	throw new UnsupportedOperationException("You should add tools.jar to classpath");
    }

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> This method first invokes the {@link
     * com.sun.tools.attach.VirtualMachineDescriptor#provider() provider()} method
     * of the given virtual machine descriptor to obtain the attach provider. It
     * then invokes the attach provider's {@link
     * com.sun.tools.attach.spi.AttachProvider#attachVirtualMachine(VirtualMachineDescriptor)
     * attachVirtualMachine} to attach to the target VM.
     *
     * @param   vmd
     *          The virtual machine descriptor.
     *
     * @return  A VirtualMachine representing the target VM.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          <tt>("attachVirtualMachine")</tt>, or another permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the attach provider's <code>attachVirtualmachine</code>
     *          throws <code>AttachNotSupportedException</code>.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>vmd</code> is <code>null</code>.
     */
    public static VirtualMachine attach(VirtualMachineDescriptor vmd) throws AttachNotSupportedException, IOException {
    	throw new UnsupportedOperationException("You should add tools.jar to classpath");
    }

    /**
     * Detach from the virtual machine.
     *
     * <p> After detaching from the virtual machine, any further attempt to invoke
     * operations on that virtual machine will cause an {@link java.io.IOException
     * IOException} to be thrown. If an operation (such as {@link #loadAgent
     * loadAgent} for example) is in progress when this method is invoked then
     * the behaviour is implementation dependent. In other words, it is
     * implementation specific if the operation completes or throws
     * <tt>IOException</tt>.
     *
     * <p> If already detached from the virtual machine then invoking this
     * method has no effect. </p>
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract void detach() throws IOException;

    /**
     * Returns the provider that created this virtual machine.
     *
     * @return  The provider that created this virtual machine.
     */
    public abstract AttachProvider provider();

    /**
     * Returns the identifier for this Java virtual machine.
     *
     * @return  The identifier for this Java virtual machine.
     */
    public abstract String id();

    /**
     * Loads an agent library.
     *
     * <p> A <a href="../../../../../../../../technotes/guides/jvmti/index.html">JVM
     * TI</a> client is called an <i>agent</i>. It is developed in a native language.
     * A JVM TI agent is deployed in a platform specific manner but it is typically the
     * platform equivalent of a dynamic library. This method causes the given agent
     * library to be loaded into the target VM (if not already loaded).
     * It then causes the target VM to invoke the <code>Agent_OnAttach</code> function
     * as specified in the
     * <a href="../../../../../../../../technotes/guides/jvmti/index.html"> JVM Tools
     * Interface</a> specification. Note that the <code>Agent_OnAttach</code>
     * function is invoked even if the agent library was loaded prior to invoking
     * this method.
     *
     * <p> The agent library provided is the name of the agent library. It is interpreted
     * in the target virtual machine in an implementation-dependent manner. Typically an
     * implementation will expand the library name into an operating system specific file
     * name. For example, on UNIX systems, the name <tt>foo</tt> might be expanded to
     * <tt>libfoo.so</tt>, and located using the search path specified by the
     * <tt>LD_LIBRARY_PATH</tt> environment variable.</p>
     *
     * <p> If the <code>Agent_OnAttach</code> function in the agent library returns
     * an error then an {@link com.sun.tools.attach.AgentInitializationException} is
     * thrown. The return value from the <code>Agent_OnAttach</code> can then be
     * obtained by invoking the {@link
     * com.sun.tools.attach.AgentInitializationException#returnValue() returnValue}
     * method on the exception. </p>
     *
     * @param   agentLibrary
     *          The name of the agent library.
     *
     * @param   options
     *          The options to provide to the <code>Agent_OnAttach</code>
     *          function (can be <code>null</code>).
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, or cannot be loaded for
     *          another reason.
     *
     * @throws  AgentInitializationException
     *          If the <code>Agent_OnAttach</code> function returns an error
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>agentLibrary</code> is <code>null</code>.
     *
     * @see     com.sun.tools.attach.AgentInitializationException#returnValue()
     */
    public abstract void loadAgentLibrary(String agentLibrary, String options)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Loads an agent library.
     *
     * <p> This convenience method works as if by invoking:
     *
     * <blockquote><tt>
     * {@link #loadAgentLibrary(String, String) loadAgentLibrary}(agentLibrary,&nbsp;null);
     * </tt></blockquote>
     *
     * @param   agentLibrary
     *          The name of the agent library.
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, or cannot be loaded for
     *          another reason.
     *
     * @throws  AgentInitializationException
     *          If the <code>Agent_OnAttach</code> function returns an error
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>agentLibrary</code> is <code>null</code>.
     */
    public abstract void loadAgentLibrary(String agentLibrary)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Load a native agent library by full pathname.
     *
     * <p> A <a href="../../../../../../../../technotes/guides/jvmti/index.html">JVM
     * TI</a> client is called an <i>agent</i>. It is developed in a native language.
     * A JVM TI agent is deployed in a platform specific manner but it is typically the
     * platform equivalent of a dynamic library. This method causes the given agent
     * library to be loaded into the target VM (if not already loaded).
     * It then causes the target VM to invoke the <code>Agent_OnAttach</code> function
     * as specified in the
     * <a href="../../../../../../../../technotes/guides/jvmti/index.html"> JVM Tools
     * Interface</a> specification. Note that the <code>Agent_OnAttach</code>
     * function is invoked even if the agent library was loaded prior to invoking
     * this method.
     *
     * <p> The agent library provided is the absolute path from which to load the
     * agent library. Unlike {@link #loadAgentLibrary loadAgentLibrary}, the library name
     * is not expanded in the target virtual machine. </p>
     *
     * <p> If the <code>Agent_OnAttach</code> function in the agent library returns
     * an error then an {@link com.sun.tools.attach.AgentInitializationException} is
     * thrown. The return value from the <code>Agent_OnAttach</code> can then be
     * obtained by invoking the {@link
     * com.sun.tools.attach.AgentInitializationException#returnValue() returnValue}
     * method on the exception. </p>
     *
     * @param   agentPath
     *          The full path of the agent library.
     *
     * @param   options
     *          The options to provide to the <code>Agent_OnAttach</code>
     *          function (can be <code>null</code>).
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, or cannot be loaded for
     *          another reason.
     *
     * @throws  AgentInitializationException
     *          If the <code>Agent_OnAttach</code> function returns an error
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>agentPath</code> is <code>null</code>.
     *
     * @see     com.sun.tools.attach.AgentInitializationException#returnValue()
     */
    public abstract void loadAgentPath(String agentPath, String options)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Load a native agent library by full pathname.
     *
     * <p> This convenience method works as if by invoking:
     *
     * <blockquote><tt>
     * {@link #loadAgentPath(String, String) loadAgentPath}(agentLibrary,&nbsp;null);
     * </tt></blockquote>
     *
     * @param   agentPath
     *          The full path to the agent library.
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, or cannot be loaded for
     *          another reason.
     *
     * @throws  AgentInitializationException
     *          If the <code>Agent_OnAttach</code> function returns an error
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>agentPath</code> is <code>null</code>.
     */
    public abstract void loadAgentPath(String agentPath)
       throws AgentLoadException, AgentInitializationException, IOException;


   /**
     * Loads an agent.
     *
     * <p> The agent provided to this method is a path name to a JAR file on the file
     * system of the target virtual machine. This path is passed to the target virtual
     * machine where it is interpreted. The target virtual machine attempts to start
     * the agent as specified by the {@link java.lang.instrument} specification.
     * That is, the specified JAR file is added to the system class path (of the target
     * virtual machine), and the <code>agentmain</code> method of the agent class, specified
     * by the <code>Agent-Class</code> attribute in the JAR manifest, is invoked. This
     * method completes when the <code>agentmain</code> method completes.
     *
     * @param   agent
     *          Path to the JAR file containing the agent.
     *
     * @param   options
     *          The options to provide to the agent's <code>agentmain</code>
     *          method (can be <code>null</code>).
     *
     * @throws  AgentLoadException
     *          If the agent does not exist, or cannot be started in the manner
     *          specified in the {@link java.lang.instrument} specification.
     *
     * @throws  AgentInitializationException
     *          If the <code>agentmain</code> throws an exception
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>agent</code> is <code>null</code>.
     */
    public abstract void loadAgent(String agent, String options)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Loads an agent.
     *
     * <p> This convenience method works as if by invoking:
     *
     * <blockquote><tt>
     * {@link #loadAgent(String, String) loadAgent}(agent,&nbsp;null);
     * </tt></blockquote>
     *
     * @param   agent
     *          Path to the JAR file containing the agent.
     *
     * @throws  AgentLoadException
     *          If the agent does not exist, or cannot be started in the manner
     *          specified in the {@link java.lang.instrument} specification.
     *
     * @throws  AgentInitializationException
     *          If the <code>agentmain</code> throws an exception
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>agent</code> is <code>null</code>.
     */
    public abstract void loadAgent(String agent)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Returns the current system properties in the target virtual machine.
     *
     * <p> This method returns the system properties in the target virtual
     * machine. Properties whose key or value is not a <tt>String</tt> are
     * omitted. The method is approximately equivalent to the invocation of the
     * method {@link java.lang.System#getProperties System.getProperties}
     * in the target virtual machine except that properties with a key or
     * value that is not a <tt>String</tt> are not included.
     *
     * <p> This method is typically used to decide which agent to load into
     * the target virtual machine with {@link #loadAgent loadAgent}, or
     * {@link #loadAgentLibrary loadAgentLibrary}. For example, the
     * <code>java.home</code> or <code>user.dir</code> properties might be
     * use to create the path to the agent library or JAR file.
     *
     * @return  The system properties
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see     java.lang.System#getProperties
     * @see     #loadAgentLibrary
     * @see     #loadAgent
     */
    public abstract Properties getSystemProperties() throws IOException;

    /**
     * Returns the current <i>agent properties</i> in the target virtual
     * machine.
     *
     * <p> The target virtual machine can maintain a list of properties on
     * behalf of agents. The manner in which this is done, the names of the
     * properties, and the types of values that are allowed, is implementation
     * specific. Agent properties are typically used to store communication
     * end-points and other agent configuration details. For example, a debugger
     * agent might create an agent property for its transport address.
     *
     * <p> This method returns the agent properties whose key and value is a
     * <tt>String</tt>. Properties whose key or value is not a <tt>String</tt>
     * are omitted. If there are no agent properties maintained in the target
     * virtual machine then an empty property list is returned.
     *
     * @return       The agent properties
     *
     * @throws       IOException
     *               If an I/O error occurs
     */
    public abstract Properties getAgentProperties() throws IOException;

    /**
     * Returns a hash-code value for this VirtualMachine. The hash
     * code is based upon the VirtualMachine's components, and satifies
     * the general contract of the {@link java.lang.Object#hashCode()
     * Object.hashCode} method.
     *
     * @return  A hash-code value for this virtual machine
     */
    public abstract int hashCode();
    
    /**
     * Tests this VirtualMachine for equality with another object.
     *
     * <p> If the given object is not a VirtualMachine then this
     * method returns <tt>false</tt>. For two VirtualMachines to
     * be considered equal requires that they both reference the same
     * provider, and their {@link VirtualMachineDescriptor#id() identifiers} are equal. </p>
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     * @param   ob   The object to which this object is to be compared
     *
     * @return  <tt>true</tt> if, and only if, the given object is
     *                a VirtualMachine that is equal to this
     *                VirtualMachine.
     */
    public abstract boolean equals(Object ob);
    
    /**
     * Returns the string representation of the <code>VirtualMachine</code>.
     */
    public abstract String toString();
}
