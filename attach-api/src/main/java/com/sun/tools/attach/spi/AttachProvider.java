package com.sun.tools.attach.spi;

import java.io.IOException;
import java.util.List;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * Compile time only mock for attach API.
 */
public abstract class AttachProvider {

    /**
     * Return this provider's name. </p>
     *
     * @return  The name of this provider
     */
    public abstract String name();

    /**
     * Return this provider's type. </p>
     *
     * @return  The type of this provider
     */
    public abstract String type();

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> A Java virtual machine is identified by an abstract identifier. The
     * nature of this identifier is platform dependent but in many cases it will be the
     * string representation of the process identifier (or pid). </p>
     *
     * <p> This method parses the identifier and maps the identifier to a Java
     * virtual machine (in an implementation dependent manner). If the identifier
     * cannot be parsed by the provider then an {@link
     * com.sun.tools.attach.AttachNotSupportedException AttachNotSupportedException}
     * is thrown. Once parsed this method attempts to attach to the Java virtual machine.
     * If the provider detects that the identifier corresponds to a Java virtual machine
     * that does not exist, or it corresponds to a Java virtual machine that does not support
     * the attach mechanism implemented by this provider, or it detects that the
     * Java virtual machine is a version to which this provider cannot attach, then
     * an <code>AttachNotSupportedException</code> is thrown. </p>
     *
     * @param  id
     *         The abstract identifier that identifies the Java virtual machine.
     *
     * @return  VirtualMachine representing the target virtual machine.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          <tt>("attachVirtualMachine")</tt>, or other permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the identifier cannot be parsed, or it corresponds to
     *          to a Java virtual machine that does not exist, or it
     *          corresponds to a Java virtual machine which this
     *          provider cannot attach.
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>id</code> is <code>null</code>
     */
    public abstract VirtualMachine attachVirtualMachine(String id)
        throws AttachNotSupportedException, IOException;

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> A Java virtual machine can be described using a {@link
     * com.sun.tools.attach.VirtualMachineDescriptor VirtualMachineDescriptor}.
     * This method invokes the descriptor's {@link
     * com.sun.tools.attach.VirtualMachineDescriptor#provider() provider()} method
     * to check that it is equal to this provider. It then attempts to attach to the
     * Java virtual machine.
     *
     * @param  vmd
     *         The virtual machine descriptor
     *
     * @return  VirtualMachine representing the target virtual machine.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          <tt>("attachVirtualMachine")</tt>, or other permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the descriptor's {@link
     *          com.sun.tools.attach.VirtualMachineDescriptor#provider() provider()} method
     *          returns a provider that is not this provider, or it does not correspond
     *          to a Java virtual machine to which this provider can attach.
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  NullPointerException
     *          If <code>vmd</code> is <code>null</code>
     */
    public abstract VirtualMachine attachVirtualMachine(VirtualMachineDescriptor vmd)
        throws AttachNotSupportedException, IOException;

    /**
     * Lists the Java virtual machines known to this provider.
     *
     * <p> This method returns a list of {@link
     * com.sun.tools.attach.VirtualMachineDescriptor} elements. Each
     * <code>VirtualMachineDescriptor</code> describes a Java virtual machine
     * to which this provider can <i>potentially</i> attach.  There isn't any
     * guarantee that invoking {@link #attachVirtualMachine(VirtualMachineDescriptor)
     * attachVirtualMachine} on each descriptor in the list will succeed.
     *
     * @return  The list of virtual machine descriptors which describe the
     *          Java virtual machines known to this provider (may be empty).
     */
    public abstract List<VirtualMachineDescriptor> listVirtualMachines();


    /**
     * Returns a list of the installed attach providers.
     *
     * <p> An AttachProvider is installed on the platform if:
     *
     * <ul>
     *   <li><p>It is installed in a JAR file that is visible to the defining
     *   class loader of the AttachProvider type (usually, but not required
     *   to be, the {@link java.lang.ClassLoader#getSystemClassLoader system
     *   class loader}).</p></li>
     *
     *   <li><p>The JAR file contains a provider configuration named
     *   <tt>com.sun.tools.attach.spi.AttachProvider</tt> in the resource directory
     *   <tt>META-INF/services</tt>. </p></li>
     *
     *   <li><p>The provider configuration file lists the full-qualified class
     *   name of the AttachProvider implementation. </p></li>
     * </ul>
     *
     * <p> The format of the provider configuration file is one fully-qualified
     * class name per line. Space and tab characters surrounding each class name,
     * as well as blank lines are ignored. The comment character is
     *  <tt>'#'</tt> (<tt>0x23</tt>), and on each line all characters following
     * the first comment character are ignored. The file must be encoded in
     * UTF-8. </p>
     *
     * <p> AttachProvider implementations are loaded and instantiated
     * (using the zero-arg constructor) at the first invocation of this method.
     * The list returned by the first invocation of this method is the list
     * of providers. Subsequent invocations of this method return a list of the same
     * providers. The list is unmodifable.</p>
     *
     * @return  A list of the installed attach providers.
     */
    public static List<AttachProvider> providers() {
    	throw new UnsupportedOperationException("You should add tools.jar to classpath");
    }
}
