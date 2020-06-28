package org.gridkit.jvmtool;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MBeanHelperTest {

    private ObjectName memoryMXBean() throws MalformedObjectNameException {
        return new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
    }

    private ObjectName osMXBean() throws MalformedObjectNameException {
        return new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
    }

    private ObjectName threadMXBean() throws MalformedObjectNameException {
        return new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
    }

    private ObjectName runtimeMXBean() throws MalformedObjectNameException {
        return new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
    }

    private ObjectName managementMXBean() throws MalformedObjectNameException {
        return new ObjectName("com.sun.management:type=HotSpotDiagnostic");
    }

    @Test
    public void test_thread_bean_describer() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.describe(threadMXBean()));
    }

    @Test
    public void test_os_bean_describer() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.describe(osMXBean()));
    }

    @Test
    public void test_memory_bean_describer() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.describe(memoryMXBean()));
    }

    @Test
    public void test_runtime_bean_describer() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.describe(runtimeMXBean()));
    }

    @Test
    public void test_management_bean_describer() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.describe(managementMXBean()));
    }

    @Test
    public void test_bean_get_set() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        helper.set(threadMXBean(), "ThreadCpuTimeEnabled", "true");
        String threadAllocatedMemoryEnabled = helper.get(threadMXBean(), Collections.singletonList("ThreadAllocatedMemoryEnabled")).get("ThreadAllocatedMemoryEnabled");
        System.out.println("ThreadCpuTimeEnabled: " + threadAllocatedMemoryEnabled);
        assertThat(threadAllocatedMemoryEnabled).isEqualTo("true");

        helper.set(threadMXBean(), "ThreadCpuTimeEnabled", "FALSE");
        threadAllocatedMemoryEnabled = helper.get(threadMXBean(), Collections.singletonList("ThreadAllocatedMemoryEnabled")).get("ThreadAllocatedMemoryEnabled");
        System.out.println("ThreadCpuTimeEnabled: " + threadAllocatedMemoryEnabled);
        assertThat(threadAllocatedMemoryEnabled).isEqualTo("true");
    }

    @Test(expected=IllegalArgumentException.class)
    public void test_bean_invalid_set() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        helper.set(managementMXBean(), "ThreadCpuTimeEnabled", "true");
    }

    @Test
    public void test_get_sys_props() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.get(runtimeMXBean(), Collections.singletonList("SystemProperties")));
    }

    @Test
    public void test_get_sys_props_wide() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        helper.setFormatingOption(MBeanHelper.FORMAT_TABLE_COLUMN_WIDTH_THRESHOLD, 200);
        System.out.println(helper.get(runtimeMXBean(), Collections.singletonList("SystemProperties")));
    }

    @Test
    public void test_get_mem_heap() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.get(memoryMXBean(), Collections.singletonList("HeapMemoryUsage")));
    }

    @Test
    public void test_get_thread_dump() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.invoke(threadMXBean(), "dumpAllThreads", "true", "true"));
    }

    @Test
    public void test_find_deadlocked_threads() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        System.out.println(helper.invoke(threadMXBean(), "findMonitorDeadlockedThreads"));
    }

    @Test
    public void test_get_vm_spec_infos() throws Exception {
        MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
        Map<String, String> attrs = helper.get(runtimeMXBean(), Arrays.asList("SpecName", "SpecVendor", "SpecVersion"));
        System.out.println(attrs);
        assertThat(attrs).containsKeys("SpecName", "SpecVendor", "SpecVersion");
    }
}
