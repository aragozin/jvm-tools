package org.gridkit.jvmtool.jmx.beans;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.jvmtool.jmx.MXStruct;
import org.gridkit.util.formating.Formats;

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
		return (Integer)getMXAttr();
	}

	@AttrName("Verbose")
	public boolean isVerbose() {
		return (Boolean)getMXAttr();
	}
	
	@PrintTemplate(
			"${bean.init}/${bean.used}/${bean.committed}/#if (${bean.max} == -1)NA#else${bean.max}#end")
	public static class MemUsage extends MXStruct {

		@AttrName("init")
		public long getInit() {
			return (Long)getMXAttr();
		}

		@AttrName("used")
		public long getUsed() {
			return (Long)getMXAttr();
		}

		@AttrName("committed")
		public long getCommitted() {
			return (Long)getMXAttr();
		}

		@AttrName("max")
		public long getMax() {
			return (Long)getMXAttr();
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(Formats.toMemorySize(getInit()));
			sb.append("/").append(Formats.toMemorySize(getUsed()));
			sb.append("/").append(Formats.toMemorySize(getCommitted()));
			if (getMax() > 0) {
				sb.append("/").append(Formats.toMemorySize(getMax()));
			}
			else {
				sb.append("/NA");
			}
			return sb.toString();
		}
	}
}
