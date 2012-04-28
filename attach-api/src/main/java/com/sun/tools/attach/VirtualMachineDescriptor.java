package com.sun.tools.attach;

import com.sun.tools.attach.spi.AttachProvider;

/**
 * Compile time only mock for attach API.
 */
public abstract class VirtualMachineDescriptor {

    /**
     * Return the <code>AttachProvider</code> that this descriptor references.
     *
     * @return The <code>AttachProvider</code> that this descriptor references.
     */
    public abstract AttachProvider provider();

    /**
     * Return the identifier component of this descriptor.
     *
     * @return  The identifier component of this descriptor.
     */
    public abstract String id();

    /**
     * Return the <i>display name</i> component of this descriptor.
     *
     * @return  The display name component of this descriptor.
     */
    public abstract String displayName();

    /**
     * Returns a hash-code value for this VirtualMachineDescriptor. The hash
     * code is based upon the descriptor's components, and satifies
     * the general contract of the {@link java.lang.Object#hashCode()
     * Object.hashCode} method.
     *
     * @return  A hash-code value for this descriptor.
     */
    public abstract int hashCode();
    
    /**
     * Tests this VirtualMachineDescriptor for equality with another object.
     *
     * <p> If the given object is not a VirtualMachineDescriptor then this
     * method returns <tt>false</tt>. For two VirtualMachineDescriptors to
     * be considered equal requires that they both reference the same
     * provider, and their {@link #id() identifiers} are equal. </p>
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     * @param   ob   The object to which this object is to be compared
     *
     * @return  <tt>true</tt> if, and only if, the given object is
     *                a VirtualMachineDescriptor that is equal to this
     *                VirtualMachineDescriptor.
     */
    public abstract boolean equals(Object ob);
    
    /**
     * Returns the string representation of the <code>VirtualMachineDescriptor</code>.
     */
    public abstract String toString();}
