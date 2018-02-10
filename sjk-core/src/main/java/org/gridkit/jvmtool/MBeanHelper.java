/**
 * Copyright 2014 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

public class MBeanHelper {
	
    public static String FORMAT_TABLE_COLUMN_WIDTH_THRESHOLD = "table.column.maxWidth";
    public static String FORMAT_COMPOSITE_FIELD_WIDTH_THRESHODL = "composite.field.maxWidth";
    
	private MBeanServerConnection mserver;
	
	private int widthThresholdTable = 40;
	private int widthThresholdComposite = 1000;
	
	public MBeanHelper(MBeanServerConnection connection) {
		this.mserver = connection;
	}
	
	public void setFormatingOption(String name, Object value) {
	    if (FORMAT_TABLE_COLUMN_WIDTH_THRESHOLD.equals(name)) {
	        widthThresholdTable = (Integer) value;
	    }
	    else if (FORMAT_COMPOSITE_FIELD_WIDTH_THRESHODL.equals(name)) {
	        widthThresholdComposite = (Integer) value;
	    }
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

	public void getAsTable(ObjectName bean, String attr, MTable table) throws Exception {
		MBeanInfo mbinfo = mserver.getMBeanInfo(bean);
		MBeanAttributeInfo ai = attrInfo(mbinfo, attr);
		if (ai == null) {
			throw new IllegalArgumentException("No such attribute '" + attr + "'");
		}
		if (!ai.isReadable()) {
			throw new IllegalArgumentException("Attribute '" + attr + "' is write-only");
		}
		Object v = mserver.getAttribute(bean, attr);
		
		if (v instanceof CompositeData[]) {
			CompositeData[] td = (CompositeData[]) v;
			if (td.length == 0) {
				return;
			}
			List<String> header = new ArrayList<String>();
			for(String f: td[0].getCompositeType().keySet()) {
				if (!header.contains(f)) {
					header.add(f);
				}
			}
			String[] hdr = header.toArray(new String[0]);
			for(Object row: td) {
				table.append(hdr, formatRow((CompositeData)row, header));
			}
			return;
		}
		else if (v instanceof CompositeData) {
			CompositeData cd = (CompositeData) v;
			List<String> header = new ArrayList<String>();
			for(String f: cd.getCompositeType().keySet()) {
				if (!header.contains(f)) {
					header.add(f);
				}
			}
			String[] hdr = header.toArray(new String[0]);
			table.append(hdr, formatRow(cd, header));
			return;
		}
		else if (v instanceof TabularData) {
			TabularData td = (TabularData) v;
			td.getTabularType().getIndexNames();
			List<String> header = new ArrayList<String>(td.getTabularType().getIndexNames());
			for(String f: td.getTabularType().getRowType().keySet()) {
				if (!header.contains(f)) {
					header.add(f);
				}
			}
			String[] hdr = header.toArray(new String[0]);
			for(Object row: td.values()) {
				table.append(hdr, formatRow((CompositeData)row, header));
			}
			return;
		}
		else {
			throw new IllegalArgumentException("Attribute is not tabular");
		}
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
	
	public String invoke(ObjectName bean, String operation, String... params) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException, MBeanException {
		MBeanInfo mbinfo = mserver.getMBeanInfo(bean);
		MBeanOperationInfo op = null;
		for(MBeanOperationInfo oi: mbinfo.getOperations()) {
			if (oi.getName().equalsIgnoreCase(operation) && oi.getSignature().length == params.length) {
				if (op != null) {
					throw new IllegalArgumentException("Ambiguous " + operation + "/" + params.length + " operatition signature for " + bean);
				}
				op = oi;
			}
		}
		if (op == null) {
			throw new IllegalArgumentException("Operation " + operation + "/" + params.length + " not found for " + bean);
		}
		Object[] args = new Object[params.length];
		String[] sig = new String[params.length];
		for(int i = 0; i != params.length; ++i) {
			args[i] = convert(params[i], op.getSignature()[i].getType());
			sig[i] = op.getSignature()[i].getType();
		}
		return format(mserver.invoke(bean, op.getName(), args, sig), op.getReturnType());
	}
	
	private String format(Object v, String type) {
		if (type.equals("void")) {
			return null;
		}
		else if (v instanceof CompositeData[]) {
			CompositeData[] td = (CompositeData[]) v;
			if (td.length == 0) {
				return "";
			}
			List<String> header = new ArrayList<String>();
			for(String f: td[0].getCompositeType().keySet()) {
				if (!header.contains(f)) {
					header.add(f);
				}
			}
			List<String[]> content = new ArrayList<String[]>();
			content.add(header.toArray(new String[0]));
			for(Object row: td) {
				content.add(formatRow((CompositeData)row, header));
			}
			return formatTable(content, widthThresholdTable, true);			
		}
		else if (v instanceof TabularData) {
			TabularData td = (TabularData) v;
			td.getTabularType().getIndexNames();
			List<String> header = new ArrayList<String>(td.getTabularType().getIndexNames());
			for(String f: td.getTabularType().getRowType().keySet()) {
				if (!header.contains(f)) {
					header.add(f);
				}
			}
			List<String[]> content = new ArrayList<String[]>();
			content.add(header.toArray(new String[0]));
			for(Object row: td.values()) {
				content.add(formatRow((CompositeData)row, header));
			}
			return formatTable(content, widthThresholdTable, true);
		}
		else if (v instanceof CompositeData) {
			CompositeData cd = (CompositeData)v;
			List<String[]> content = new ArrayList<String[]>();
			for(String field: cd.getCompositeType().keySet()) {
				String val = formatLine(cd.get(field), cd.getCompositeType().getType(field).getClassName());
				content.add(new String[]{field + ": ", val});
			}
			return formatTable(content, widthThresholdComposite, false);
		}
		else {
			return formatLine(v, type);
		}
	}

	private String formatTable(List<String[]> content, int maxCell, boolean table) {
		int[] width = new int[content.get(0).length];
		for(String[] row: content) {
			for(int i = 0; i != row.length; ++i) {
				width[i] = Math.min(Math.max(width[i], row[i].length()), maxCell);
			}
		}

		StringBuilder sb = new StringBuilder();
		boolean header = table;
		for(String[] row: content) {
			for(int i = 0; i != width.length; ++i) {
				String cell = row[i];
				if (cell.length() > width[i]) {
					cell = cell.substring(0, width[i] - 3) + "...";
				}
				sb.append(cell);
				for(int s = 0; s != width[i] - cell.length(); ++s) {
					sb.append(' ');
				}
				if (table) {
					sb.append('|');
				}
			}
			if (table) {
				sb.setLength(sb.length() - 1);
			}
			sb.append('\n');
			if (header) {
				header = false;
				for(int n: width) {
					for(int i = 0; i != n; ++i) {
						sb.append('-');
					}
					sb.append('+');
				}
				sb.setLength(sb.length() - 1);
				sb.append('\n');
			}
		}
		
		return sb.toString();
	}
	
	private String formatLine(Object v, String type) {
		if (v instanceof TabularData) {
			TabularData td = (TabularData)v;
			StringBuilder sb = new StringBuilder();
			for(Object c: td.values()) {
				sb.append(formatLine(c, td.getTabularType().getRowType().getClassName()));
				sb.append(",");
			}
			if (sb.length() > 0) {
				sb.setLength(sb.length() - 1);
			}
			return sb.toString();
		}
		if (v instanceof CompositeData[]) {
			CompositeData[] td = (CompositeData[])v;
			StringBuilder sb = new StringBuilder();
			for(Object c: td) {
				sb.append(formatLine(c, ((CompositeData)c).getCompositeType().getClassName()));
				sb.append(",");
			}
			if (sb.length() > 0) {
				sb.setLength(sb.length() - 1);
			}
			return sb.toString();
		}
		else if (v instanceof CompositeData) {
			CompositeData cdata = (CompositeData) v;
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for(String attr: cdata.getCompositeType().keySet()) {
				sb.append(attr).append("=");
				sb.append(formatLine(cdata.get(attr), cdata.getCompositeType().getType(attr).getClassName()));
				sb.append(',');
			}
			if (sb.length() > 1) {
				sb.setLength(sb.length() - 1);
			}
			sb.append("}");
			return sb.toString();
		}
		else if (v instanceof Object[]) {
			return Arrays.toString((Object[])v);
		}
		else if (v instanceof boolean[]) {
			return Arrays.toString((boolean[])v);
		}
		else if (v instanceof byte[]) {
			return Arrays.toString((byte[])v);
		}
		else if (v instanceof char[]) {
			return Arrays.toString((char[])v);
		}
		else if (v instanceof short[]) {
			return Arrays.toString((short[])v);
		}
		else if (v instanceof int[]) {
			return Arrays.toString((int[])v);
		}
		else if (v instanceof long[]) {
			return Arrays.toString((long[])v);
		}
		else if (v instanceof float[]) {
			return Arrays.toString((float[])v);
		}
		else if (v instanceof double[]) {
			return Arrays.toString((double[])v);
		}
		else {
			return String.valueOf(v);
		}
	}

	private String[] formatRow(CompositeData row, List<String> header) {
		String[] text = new String[header.size()];
		for(int i = 0; i != text.length; ++i) {
			String attr = header.get(i);
			text[i] = formatLine(row.get(attr), row.getCompositeType().getType(attr).getClassName());
		}
		return text;
	}

	private Object convert(String value, String type) {
		if (type.equals("java.lang.String")) {
			if ("<null>".equals(value.trim()))
				return null;
			return value;
		}
		if (type.equals("boolean") || type.equals("java.lang.Boolean")) {
			if (type.equals("java.lang.Boolean") && value.trim().isEmpty())
				return null;
			return Boolean.valueOf(value);
		}
		else if (type.equals("byte") || type.equals("java.lang.Byte")) {
			if (type.equals("java.lang.Byte") && value.trim().isEmpty())
				return null;
			return Byte.valueOf(value);
		}
		else if (type.equals("short") || type.equals("java.lang.Short")) {
			if (type.equals("java.lang.Short") && value.trim().isEmpty())
				return null;
			return Short.valueOf(value);
		}
		else if (type.equals("char") || type.equals("java.lang.Character")) {
			if (type.equals("java.lang.Character") && value.trim().isEmpty())
				return null;
			if (value.length() == 1) {
				return value.charAt(0);
			}
			else {
				throw new IllegalArgumentException("Cannot convert '" + value + "' to " + type);
			}
		}
		else if (type.equals("int") || type.equals("java.lang.Integer")) {
			if (type.equals("java.lang.Integer") && value.trim().isEmpty())
				return null;
			return Integer.valueOf(value);
		}
		else if (type.equals("long") || type.equals("java.lang.Long")) {
			if (type.equals("java.lang.Long") && value.trim().isEmpty())
				return null;
			return Long.valueOf(value);
		}
		else if (type.equals("float") || type.equals("java.lang.Float")) {
			if (type.equals("java.lang.Float") && value.trim().isEmpty())
				return null;
			return Float.valueOf(value);
		}
		else if (type.equals("double") || type.equals("java.lang.Double")) {
			if (type.equals("java.lang.Double") && value.trim().isEmpty())
				return null;
			return Double.valueOf(value);
		}
		else if (type.startsWith("[")) {
			if (value.trim().isEmpty())
				return null;
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
