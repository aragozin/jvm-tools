package org.gridkit.jvmtool.jmx;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

public abstract class MXStruct implements Cloneable {

	public static ObjectName name(String name) {
		try {
			return new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Map<String, AttrInfo> meta;
	private String[] attrNames;
	private Map<String, Object> data;
	private String printTemplate;
	
	protected MXStruct() {
		meta = collectMeta();
		attrNames = new String[meta.size()];
		int n = 0;
		for(AttrInfo ai: meta.values()) {
			attrNames[n++] = ai.attrName;
		}
	}

	private Map<String, AttrInfo> collectMeta() {
		
		Map<String, AttrInfo> meta = new LinkedHashMap<String, MXStruct.AttrInfo>();
		
		for(Method m : getClass().getMethods()) {
			if (m.isAnnotationPresent(AttrName.class)) {
				AttrName an = m.getAnnotation(AttrName.class);
				AsObject ot = m.getAnnotation(AsObject.class);
				AsCollection ct = m.getAnnotation(AsCollection.class);
				AsMap mt = m.getAnnotation(AsMap.class);
				int ac = (ot == null ? 0 : 1)
						+(ct == null ? 0 : 1)
						+(mt == null ? 0 : 1);
				
				if (ac > 1) {
					throw new IllegalArgumentException("You can specify only one \"As\" annotation for method");
				}
				AttrInfo ai = new AttrInfo();
				ai.methodName = m.getName();
				ai.attrName = an.value();
				
				if (ot != null) {
					Class<?> t = ot.value();
					ai.converter = createConverter(t);
				}
				else if (ct != null) {
					Class<?> t = ct.value();
					ai.converter = new CollectionConverter(createConverter(t));
				}
				else if (mt != null) {
					String key = mt.key();
					String val = mt.val();
					Class<?> t =mt.type();
					ai.converter = new MapConverter(key, val, createConverter(t));
				}
				else if (MXStruct.class.isAssignableFrom(m.getReturnType())) {
					Class<?> t = m.getReturnType();
					ai.converter = createConverter(t);					
				}
				meta.put(ai.methodName, ai);
			}
		}
		
		PrintTemplate pt = getClass().getAnnotation(PrintTemplate.class);
		if (pt != null) {
			printTemplate = pt.value();
		}
		
		return meta;
	}

	private Converter createConverter(Class<?> t) {
		if (MXStruct.class.isAssignableFrom(t)) {
			return new StructConverter(t);
		}
		else if (t == Void.class) {
			return null;
		}
		else {
			throw new IllegalArgumentException("Class " + t.getName() + " is not an " + MXStruct.class.getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	public <V extends MXStruct> V read(MBeanServerConnection conn, ObjectName name) throws ReflectionException, IOException {
		try {
			MXStruct that;
			that = (MXStruct) clone();
			that.meta = meta;
			that.attrNames = attrNames;
			that.printTemplate = printTemplate;
			that.data = new LinkedHashMap<String, Object>();
			try {
				AttributeList al = conn.getAttributes(name, attrNames);
				for (Attribute attr: al.asList()) {
					that.data.put(attr.getName(), attr.getValue());
				}
				return (V) that;
			}
			catch(InstanceNotFoundException e) {
				return null;
			}			
		} catch (CloneNotSupportedException e) {
			throw new Error("It IS clonable");
		}
	}
	
	@SuppressWarnings("unchecked")
	public <V extends MXStruct> V read(CompositeData cdata) {
		MXStruct that;
		try {
			that = (MXStruct) clone();
			that.meta = meta;
			that.attrNames = attrNames;
			that.data = new LinkedHashMap<String, Object>();
			for(String attr : attrNames) {
				that.data.put(attr, cdata.get(attr));
			}
			return (V)that;
		} catch (CloneNotSupportedException e) {
			throw new Error("It IS clonable");
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <V> V getMXAttr() {
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		String methodName = trace[2].getMethodName();
		AttrInfo info = meta.get(methodName);
		if (info == null) {
			throw new IllegalArgumentException("Method " + methodName + " is not annotated with MBean meta data");
		}
		Object val = data.get(info.attrName);
		if (val != null && info.converter != null) {
			return (V) info.converter.convert(val);
		}
		else {
			return (V) val;
		}
	}
	
	private class AttrInfo {
		
		private String methodName;
		private String attrName;
		private Converter converter;
		
	}
	
	private interface Converter {
		
		public Object convert(Object val);
		
	}	
	
	private static class StructConverter implements Converter {
		
		private MXStruct proto;

		public StructConverter(Class<?> type) {
			try {
				this.proto = (MXStruct) type.newInstance();
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("Cannot instantiate", e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("Cannot instantiate", e);
			}
		}

		@Override
		public Object convert(Object val) {
			return val == null ? null : proto.read((CompositeData) val);
		}
	}
	
	private static class CollectionConverter implements Converter {
		
		private Converter elementConverter;

		public CollectionConverter(Converter elementConverter) {
			this.elementConverter = elementConverter;
		}

		@Override
		public Object convert(Object val) {
			if (val == null) {
				return null;
			}
			else {
				List<Object> x = new ArrayList<Object>();
				for(Object o : (Collection<?>)val) {
					x.add(elementConverter == null ? o : elementConverter.convert(o));
				}
				return x;
			}
		}
	}

	private static class MapConverter implements Converter {
		
		private String keyAttr;
		private String valueAttr;
		private Converter elementConverter;
		
		public MapConverter(String keyAttr, String valueAttr, Converter elementConverter) {
			this.keyAttr = normString(keyAttr);
			this.valueAttr = normString(valueAttr);
			this.elementConverter = elementConverter;
		}

		private String normString(String valueAttr) {
			valueAttr = valueAttr.trim();
			return (valueAttr != null && valueAttr.length() > 1) ? valueAttr : null;
		}

		@Override
		public Object convert(Object val) {
			if (val == null) {
				return null;
			}
			else {
				Map<Object, Object> x = new LinkedHashMap<Object, Object>();
				if (keyAttr != null) {
					// folded map
					for(Object o : (Collection<?>)val) {
						CompositeData cdata = (CompositeData) o;
						Object key = cdata.get(keyAttr);
						Object v = valueAttr == null ? cdata : cdata.get(valueAttr);
						if (elementConverter != null) {
							v = elementConverter.convert(v);
						}
						x.put(key, v);
					}
				}
				else {
					// natural map
					for(Map.Entry<?, ?> e: ((Map<?, ?>)val).entrySet()) {
						Object key = e.getValue();
						Object v = valueAttr == null ? e.getValue() : ((CompositeData)e.getValue()).get(valueAttr);
						if (elementConverter != null) {
							v = elementConverter.convert(v);
						}
						x.put(key, v);
					}
				}
				return x;
			}
		}
	}
	
	@Override
	public String toString() {
		if (printTemplate != null && printTemplate.length() > 0) {
			VelocityContext ctx = new VelocityContext();
			ctx.internalPut("bean", this);
			StringWriter sw = new StringWriter();
			Velocity.evaluate(ctx, sw, getClass().getSimpleName() + ": cannot print", printTemplate);
			return sw.toString();
		}
		else {
			return getClass().getSimpleName() + ":" + data.toString();
		}
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	protected @interface AttrName {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	protected @interface AsObject {
		Class<?> value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	protected @interface AsCollection {
		Class<?> value() default Void.class;
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	protected @interface AsMap {
		String key() default "";
		String val() default "";
		Class<?> type() default Void.class;
	}

	@Retention(RetentionPolicy.RUNTIME)
	protected @interface PrintTemplate {
		String value();
	}
}
