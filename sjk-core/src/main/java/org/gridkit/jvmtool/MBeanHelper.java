package org.gridkit.jvmtool;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public class MBeanHelper {
	
	private MBeanServerConnection mserver;
	
	public MBeanHelper(MBeanServerConnection connection) {
		this.mserver = connection;
	}
	
	public String describe(ObjectName bean) throws Exception {
		MBeanInfo mbinfo = mserver.getMBeanInfo(bean);
		StringBuilder sb = new StringBuilder();
		sb.append(bean);
		sb.append('\n');
		sb.append(mbinfo.getClassName());
		sb.append('\n');
		sb.append(" - " + mbinfo.getDescription());
		sb.append('\n');
		for(MBeanAttributeInfo ai: mbinfo.getAttributes()) {
			sb.append(" (A) ");
			sb.append(ai.getName()).append(" : ").append(toPrintableType(ai.getType())).append("");
			if (!ai.isReadable()) {
				sb.append(" - WRITEONLY");
			}
			else if (ai.isWritable()) {
				sb.append(" - WRITEABLE");
			}
			sb.append('\n');
			if (!ai.getName().equals(ai.getDescription())) {
				sb.append("  - " + ai.getDescription());
				sb.append('\n');
			}
		}
		for (MBeanOperationInfo oi: mbinfo.getOperations()) {
			sb.append(" (O) ");
			sb.append(oi.getName()).append("(");
			for(MBeanParameterInfo pi: oi.getSignature()) {
				String name = pi.getName();
				String type = toPrintableType(pi.getType());
				sb.append(type).append(' ').append(name).append(", ");
			}
			if (oi.getSignature().length > 0) {
				sb.setLength(sb.length() - 2);
			}
			sb.append(") : ").append(toPrintableType(oi.getReturnType()));
			sb.append('\n');
			if (!oi.getName().equals(oi.getDescription())) {
				sb.append("  - " + oi.getDescription());
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	static Map<String, String> TYPE_MAP = new HashMap<String, String>();
	static {
		TYPE_MAP.put("javax.management.openmbean.CompositeData", "CompositeData");
		TYPE_MAP.put("[Z", "boolean[]");
		TYPE_MAP.put("[B", "byte[]");
		TYPE_MAP.put("[S", "short[]");
		TYPE_MAP.put("[C", "char[]");
		TYPE_MAP.put("[I", "int[]");
		TYPE_MAP.put("[J", "long[]");
		TYPE_MAP.put("[F", "float[]");
		TYPE_MAP.put("[D", "double[]");
	}
	
	static String toPrintableType(String type) {
		if (TYPE_MAP.containsKey(type)) {
			return TYPE_MAP.get(type);
		}
		else if (type.startsWith("[L")) {
			return toPrintableType(type.substring(2, type.length() -1)) + "[]";
		}
		else {
			return type;
		}
	}
	
}
