package org.gridkit.jvmtool.agent;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import sun.management.HotspotInternal;

@SuppressWarnings("restriction")
public class HotspotInternalMBeanEnabler implements AgentCmd {

    @Override
    public void start(Properties agentProps, String agentArgs, Instrumentation inst) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

        try {
            String mname = "sun.management:type=HotspotThreading";
            MBeanInfo info = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(new ObjectName(mname));
            if (info != null) {
                // bean is present
                agentProps.put(this.getClass().getName() + ".enabled", "true");
                return;
            }
        } catch (Exception e) {
            // ignore
        }

        HotspotInternal hi = new HotspotInternal();
        ManagementFactory.getPlatformMBeanServer().registerMBean(hi, null);
        agentProps.put(this.getClass().getName() + ".enabled", "true");
    }
}
