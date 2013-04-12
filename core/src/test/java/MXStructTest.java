import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;

import org.gridkit.jvmtool.jmx.JmxHelper;
import org.gridkit.jvmtool.jmx.beans.GarbageCollectorMXStruct;
import org.gridkit.jvmtool.jmx.beans.GarbageCollectorMXStruct.LastGcInfo;
import org.gridkit.jvmtool.jmx.beans.MemoryMXStruct;
import org.gridkit.jvmtool.jmx.beans.MemoryMXStruct.MemUsage;
import org.gridkit.jvmtool.jmx.beans.MemoryPoolMXStruct;
import org.testng.Assert;
import org.testng.annotations.Test;


public class MXStructTest {

	private MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
	
	@Test
	public void test_all_attrs() throws ReflectionException, IOException {
		MemoryMXStruct membean = MemoryMXStruct.PROTO.read(conn, MemoryMXStruct.NAME);
		
		membean.getHeapMemoryUsage().getInit();
		membean.getHeapMemoryUsage().getUsed();
		membean.getHeapMemoryUsage().getCommitted();
		membean.getHeapMemoryUsage().getMax();

		membean.getNonHeapMemoryUsage().getInit();
		membean.getNonHeapMemoryUsage().getUsed();
		membean.getNonHeapMemoryUsage().getCommitted();
		membean.getNonHeapMemoryUsage().getMax();
		
		String heapUsage = membean.getHeapMemoryUsage().toString();
		
		membean.getObjectPendingFinalizationCount();
		membean.isVerbose();
	}
	
	@Test
	public void test_gc_beans() throws MalformedObjectNameException, NullPointerException, IOException, ReflectionException {		
		Map<String, GarbageCollectorMXStruct> beans = JmxHelper.collectBeans(conn, GarbageCollectorMXStruct.PATTERN, GarbageCollectorMXStruct.PROTO);
		Assert.assertTrue(beans.size() > 0);
		for(GarbageCollectorMXStruct bean: beans.values()) {
			bean.getName();
			bean.getCollectionCount();
			bean.getCollectionTime();
			bean.getMemoryPoolNames();
			bean.isValid();
			
			LastGcInfo lastGcInfo = bean.getLastGcInfo();
			lastGcInfo.getGcThreadCount();
			lastGcInfo.getStartTime();
			lastGcInfo.getEndTime();
			lastGcInfo.getDuration();
			
			Map<String, MemUsage> bgc = lastGcInfo.getMemoryUsageBeforeGc();
			Assert.assertTrue(bgc.size() > 0);
			for(MemUsage mu: bgc.values()) {
				mu.getInit();
				mu.getUsed();
				mu.getCommitted();
				mu.getMax();
			}

			Map<String, MemUsage> agc = lastGcInfo.getMemoryUsageAfterGc();
			Assert.assertTrue(agc.size() > 0);
			for(MemUsage mu: agc.values()) {
				mu.getInit();
				mu.getUsed();
				mu.getCommitted();
				mu.getMax();
			}
		}
	}

	@Test
	public void test_memory_pool_beans() throws MalformedObjectNameException, NullPointerException, IOException, ReflectionException {		
		Map<String, MemoryPoolMXStruct> beans = JmxHelper.collectBeans(conn, MemoryPoolMXStruct.PATTERN, MemoryPoolMXStruct.PROTO);
		Assert.assertTrue(beans.size() > 0);
		for(MemoryPoolMXStruct bean: beans.values()) {
			bean.getName();
			bean.getType();
			bean.getMemoryManagerNames();
			bean.getUsage().getInit();
			bean.getUsage().getUsed();
			bean.getUsage().getCommitted();
			bean.getUsage().getMax();
			bean.getPeakUsage().getInit();
			bean.getPeakUsage().getUsed();
			bean.getPeakUsage().getCommitted();
			bean.getPeakUsage().getMax();
			bean.isValid();
		}
	}	
	
	@Test
	public void test_threading_mbean() {
		
	}
}
