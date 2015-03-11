package org.gridkit.jvmtool.stacktrace;

/**
 * Additional methods available in modern JVMs.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ThreadMXBeanEx extends java.lang.management.ThreadMXBean {

    public long[] getThreadCpuTime(long[] ids);

    public long[] getThreadUserTime(long[] ids);

    public long[] getThreadAllocatedBytes(long[] ids);

}
