package org.gridkit.jvmtool.jmx.beans;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.jvmtool.jmx.MXStruct;

public class MemoryMXStruct extends MXStruct{

	public static final ObjectName NAME = name("java.lang:type=Memory");
	public static final MemoryMXStruct PROTO = new MemoryMXStruct();
	
	public static MemoryMXStruct get(MBeanServerConnection conn) throws ReflectionException, IOException {
		return PROTO.read(conn, NAME);
	}
	
	@AttrName("HeapMemoryUsage") @AsObject(MemUsage.class)
	public MemUsage getHeapMemoryUsage() {
		return getMXAttr();
	}
	
	@AttrName("NonHeapMemoryUsage") @AsObject(MemUsage.class)
	public MemUsage getNonHeapMemoryUsage() {
		return getMXAttr();
	}
	
	@AttrName("ObjectPendingFinalizationCount")
	public int getObjectPendingFinalizationCount() {
		return getMXAttr();
	}

	@AttrName("Verbose")
	public boolean isVerbose() {
		return getMXAttr();
	}
	
	@PrintTemplate(
			"${bean.init}/${bean.used}/${bean.committed}/#if (${bean.max} == -1)NA#else${bean.max}#end")
	public static class MemUsage extends MXStruct {

		@AttrName("init")
		public long getInit() {
			return getMXAttr();
		}

		@AttrName("used")
		public long getUsed() {
			return getMXAttr();
		}

		@AttrName("committed")
		public long getCommitted() {
			return getMXAttr();
		}

		@AttrName("max")
		public long getMax() {
			return getMXAttr();
		}
	}
}
