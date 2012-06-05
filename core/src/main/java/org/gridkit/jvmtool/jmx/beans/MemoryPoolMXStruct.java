package org.gridkit.jvmtool.jmx.beans;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.gridkit.jvmtool.jmx.JmxHelper;
import org.gridkit.jvmtool.jmx.MXStruct;
import org.gridkit.jvmtool.jmx.beans.MemoryMXStruct.MemUsage;

public class MemoryPoolMXStruct extends MXStruct {

	public static final ObjectName PATTERN = name("java.lang:type=MemoryPool,name=*");
	public static final MemoryPoolMXStruct PROTO = new MemoryPoolMXStruct();
	
	public static Map<String, MemoryPoolMXStruct> get(MBeanServerConnection conn) throws ReflectionException, IOException {
		return JmxHelper.collectBeans(conn, PATTERN, PROTO);
	}
	
	@AttrName("Name")
	public String getName() {
		return getMXAttr();
	}
	
	@AttrName("Type")
	public String getType() {
		return getMXAttr();
	}

	@AttrName("Usage")
	public MemUsage getUsage() {
		return getMXAttr();
	}

	@AttrName("PeakUsage")
	public MemUsage getPeakUsage() {
		return getMXAttr();
	}
	
	@AttrName("MemoryManagerNames")
	public String[] getMemoryManagerNames() {
		return getMXAttr();
	}
	
	@AttrName("Valid")
	public boolean isValid() {
		return getMXAttr();
	}	
}
