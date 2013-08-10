package org.gridkit.jvmtool;

import java.lang.management.ManagementFactory;

import javax.management.ObjectName;

import org.junit.Test;

public class MBeanHelperTest {

	@Test
	public void test_bean_describer() throws Exception {
		MBeanHelper helper = new MBeanHelper(ManagementFactory.getPlatformMBeanServer());
		System.out.println(helper.describe(new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME)));
	}
}
