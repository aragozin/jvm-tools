package org.gridkit.jvmtool.jmx.beans;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.jvmtool.jmx.JmxHelper;
import org.gridkit.jvmtool.jmx.MXStruct;
import org.gridkit.jvmtool.jmx.beans.MemoryMXStruct.MemUsage;

public class GarbageCollectorMXStruct extends MXStruct {
	
	public static final ObjectName PATTERN = name("java.lang:type=GarbageCollector,name=*");
	public static final GarbageCollectorMXStruct PROTO = new GarbageCollectorMXStruct();
	
	public static Map<String, GarbageCollectorMXStruct> get(MBeanServerConnection conn) throws ReflectionException, IOException {
		return JmxHelper.collectBeans(conn, PATTERN, PROTO);
	}
	
	@AttrName("Name")
	public String getName() {
		return getMXAttr();
	}
	
	@AttrName("Valid")
	public boolean isValid() {
		return (Boolean)getMXAttr();
	}
	
	@AttrName("CollectionCount")
	public long getCollectionCount() {
		return (Long)getMXAttr();
	}

	@AttrName("CollectionTime")
	public long getCollectionTime() {
		return (Long)getMXAttr();
	}
	
	@AttrName("MemoryPoolNames")
	public String[] getMemoryPoolNames() {
		return getMXAttr();
	}
	
	@AttrName("LastGcInfo")
	public LastGcInfo getLastGcInfo() {
		return getMXAttr();
	}
	
	public static class LastGcInfo extends MXStruct {
		
		@AttrName("GcThreadCount")
		public int getGcThreadCount() {
			return (Integer)getMXAttr();
		}
		
		@AttrName("duration")
		public long getDuration() {
			return (Long)getMXAttr();
		}
		
		@AttrName("startTime")
		public long getStartTime() {
			return  (Long)getMXAttr();
		}
		
		@AttrName("endTime")
		public long getEndTime() {
			return  (Long)getMXAttr();
		}
		
		@AttrName("memoryUsageBeforeGc") @AsMap(val="value", type=MemUsage.class)
		public Map<String, MemUsage> getMemoryUsageBeforeGc() {
			return getMXAttr();
		}

		@AttrName("memoryUsageAfterGc") @AsMap(val="value", type=MemUsage.class)
		public Map<String, MemUsage> getMemoryUsageAfterGc() {
			return getMXAttr();
		}
	}
}
