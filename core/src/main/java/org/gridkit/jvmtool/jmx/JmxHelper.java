package org.gridkit.jvmtool.jmx;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class JmxHelper {

	public static <V extends MXStruct> Map<String, V> collectBeans(MBeanServerConnection conn, ObjectName pattern, V proto) throws IOException, ReflectionException {
		Map<String, V> result = new LinkedHashMap<String, V>();
		for(ObjectName name: conn.queryNames(pattern, null)) {
			StringBuffer sb = new StringBuffer();
			for(String key: pattern.getKeyPropertyList().keySet()) {
				String val = pattern.getKeyProperty(key);
				if ("*".equals(val)) {
					if (sb.length() > 0) {
						sb.append(',');
					}
					sb.append(name.getKeyProperty(key));
				}				
			}
			V mstruct = proto.read(conn, name);
			result.put(sb.toString(), mstruct);
		}
		return result;
	}	
}
