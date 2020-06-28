package org.gridkit.jvmtool.stacktrace;

import java.lang.management.ThreadMXBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Additional methods available in modern JVMs.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ThreadMXBeanEx extends java.lang.management.ThreadMXBean {

    public static final ObjectName THREADING_MBEAN = BeanHelper.name("java.lang:type=Threading");

    public long[] getThreadCpuTime(long[] ids);

    public long[] getThreadUserTime(long[] ids);

    public long[] getThreadAllocatedBytes(long[] ids);

    public static class BeanHelper {

        private static ObjectName name(String name) {
            try {
                return new ObjectName(name);
            } catch (MalformedObjectNameException e) {
                throw new RuntimeException(e);
            }
        }

        public static ThreadMXBean connectThreadMXBean(MBeanServerConnection mserver) {
            ThreadMXBean bean;
            try {
                bean = JMX.newMXBeanProxy(mserver, THREADING_MBEAN, ThreadMXBeanEx.class);
            } catch(Exception e) {
                bean = JMX.newMXBeanProxy(mserver, THREADING_MBEAN, ThreadMXBean.class);
            }
            return bean;
        }
    }
}
