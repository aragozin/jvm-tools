package org.gridkit.jvmtool;

import java.lang.management.ManagementFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.Test;

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
		helper.set(threadMXBean(), "ThreadAllocatedMemoryEnabled", "true");
		System.out.println("ThreadAllocatedMemoryEnabled: " + helper.get(threadMXBean(), "ThreadAllocatedMemoryEnabled"));
		helper.set(threadMXBean(), "ThreadAllocatedMemoryEnabled", "FALSE");
		System.out.println("ThreadAllocatedMemoryEnabled: " + helper.get(threadMXBean(), "ThreadAllocatedMemoryEnabled"));
	}

	@Test
	public void test_bean_invalid_set() throws Exception {
		MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
		helper.set(managementMXBean(), "ThreadAllocatedMemoryEnabled", "true");
	}

	@Test
	public void test_get_sys_props() throws Exception {
		MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
		System.out.println(helper.get(runtimeMXBean(), "SystemProperties"));
	}
}
