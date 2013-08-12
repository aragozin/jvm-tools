package org.gridkit.jvmtool;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
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
	
	public String get(ObjectName bean, String attr) throws Exception {
		MBeanInfo mbinfo = mserver.getMBeanInfo(bean);
		MBeanAttributeInfo ai = attrInfo(mbinfo, attr);
		if (ai == null) {
			throw new IllegalArgumentException("No such attribute '" + attr + "'");
		}
		if (!ai.isReadable()) {
			throw new IllegalArgumentException("Attribute '" + attr + "' is write-only");
		}
		Object v = mserver.getAttribute(bean, attr);
		String type = ai.getType();
		String text = format(v, type);
		return text;
	}

	public void set(ObjectName bean, String attr, String value) throws Exception {
		MBeanInfo mbinfo = mserver.getMBeanInfo(bean);
		MBeanAttributeInfo ai = attrInfo(mbinfo, attr);
		if (ai == null) {
			throw new IllegalArgumentException("No such attribute '" + attr + "'");
		}
		if (!ai.isWritable()) {
			throw new IllegalArgumentException("Attribute '" + attr + "' is not writeable");
		}
		String type = ai.getType();
		Object ov = convert(value, type);
		mserver.setAttribute(bean, new Attribute(attr, ov));
	}
	
	private String format(Object v, String type) {
//		if (v instanceof TabularData) {
//			
//		}
//		else if (v instanceof CompositeData) {
//			
//		}
//		else {
			return String.valueOf(v);
//		}
	}

	private String formatLine(Object v, String type) {
		return String.valueOf(v);
	}
	
	private Object convert(String value, String type) {
		if (type.equals("java.lang.String")) {
			return value;
		}
		if (type.equals("boolean")) {
			return Boolean.valueOf(value);
		}
		else if (type.equals("byte")) {
			return Byte.valueOf(value);
		}
		else if (type.equals("short")) {
			return Short.valueOf(value);
		}
		else if (type.equals("char")) {
			if (value.length() == 1) {
				return value.charAt(0);
			}
			else {
				throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type);
			}
		}
		else if (type.equals("int")) {
			return Integer.valueOf(value);
		}
		else if (type.equals("long")) {
			return Long.valueOf(value);
		}
		else if (type.equals("float")) {
			return Float.valueOf(value);
		}
		else if (type.equals("double")) {
			return Double.valueOf(value);
		}
		else if (type.startsWith("[")) {
			String[] elements = value.split("[,]");
			Object array = ARRAY_MAP.get(type);
			if (array == null) {
				throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type);
			}
			array = Array.newInstance(array.getClass().getComponentType(), elements.length);
			String etype = array.getClass().getComponentType().getName();
			for(int i = 0; i != elements.length; ++i) {
				Array.set(array, i, convert(elements[i], etype));
			}
			return array;			
		}
		throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type);
	}

	private MBeanAttributeInfo attrInfo(MBeanInfo mbinfo, String attr) {
		for(MBeanAttributeInfo ai: mbinfo.getAttributes()) {
			if (ai.getName().equals(attr)) {
				return ai;
			}
		}
		return null;
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

	static Map<String, Object> ARRAY_MAP = new HashMap<String, Object>();
	static {
		ARRAY_MAP.put("[Z", new boolean[0]);
		ARRAY_MAP.put("[B", new byte[0]);
		ARRAY_MAP.put("[S", new short[0]);
		ARRAY_MAP.put("[C", new char[0]);
		ARRAY_MAP.put("[I", new int[0]);
		ARRAY_MAP.put("[J", new long[0]);
		ARRAY_MAP.put("[F", new float[0]);
		ARRAY_MAP.put("[D", new double[0]);
		ARRAY_MAP.put("[Ljava.lang.String;", new String[0]);
	}

	static Map<String, String> TYPE_MAP = new HashMap<String, String>();
	static {
		TYPE_MAP.put("java.lang.String", "String");
		TYPE_MAP.put("javax.management.openmbean.CompositeData", "CompositeData");
		TYPE_MAP.put("javax.management.openmbean.TabularData", "TabularData");
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
