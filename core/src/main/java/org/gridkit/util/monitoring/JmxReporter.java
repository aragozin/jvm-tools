// Copyright (c) 2012 Cloudera, Inc. All rights reserved.
package org.gridkit.util.monitoring;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.sun.tools.attach.VirtualMachine;

/**
 * Dumps JMX data from pid as JSON.
 */
public class JmxReporter {

	static {
		AttachUtil.ensureToolsClasspath();
	}

	public static void main(String[] args) throws Exception {

		String pid = args[0];
        VirtualMachine jvm = AttachUtil.attachToPid(pid);
        MBeanServerConnection jmx = AttachUtil.attachJmx(jvm);
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jg = jsonFactory.createJsonGenerator(System.out);
        jg.useDefaultPrettyPrinter();
        jg.writeStartObject();
        listBeans(jg, jmx);
        jg.writeEndObject();
        jg.close();
        System.out.println();
        System.out.flush();
    }
    
    

    private static void listBeans(JsonGenerator jg, MBeanServerConnection mBeanServer) throws Exception {
        Set<ObjectName> names = null;
        names = mBeanServer.queryNames(null, null);

        jg.writeArrayFieldStart("beans");
        Iterator<ObjectName> it = names.iterator();
        while (it.hasNext()) {
            ObjectName oname = it.next();
            MBeanInfo minfo;
            String code = "";
            minfo = mBeanServer.getMBeanInfo(oname);
            code = minfo.getClassName();
            String prs = "";
            if ("org.apache.commons.modeler.BaseModelMBean".equals(code)) {
                prs = "modelerType";
                code = (String) mBeanServer.getAttribute(oname, prs);
            }

            jg.writeStartObject();
            jg.writeStringField("name", oname.toString());

            jg.writeStringField("modelerType", code);

            MBeanAttributeInfo attrs[] = minfo.getAttributes();
            for (int i = 0; i < attrs.length; i++) {
                writeAttribute(mBeanServer, jg, oname, attrs[i]);
            }
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    private static void writeAttribute(MBeanServerConnection mBeanServer, JsonGenerator jg, ObjectName oname, MBeanAttributeInfo attr) throws IOException {
        if (!attr.isReadable()) {
            return;
        }
        String attName = attr.getName();
        if ("modelerType".equals(attName)) {
            return;
        }
        if (attName.indexOf("=") >= 0 || attName.indexOf(":") >= 0
                || attName.indexOf(" ") >= 0) {
            return;
        }
        Object value = null;
        try {
            value = mBeanServer.getAttribute(oname, attName);
        } catch (AttributeNotFoundException e) {
            //Ignored the attribute was not found, which should never happen because the bean
            //just told us that it has this attribute, but if this happens just don't output
            //the attribute.
            return;
        } catch (MBeanException e) {
            //The code inside the attribute getter threw an exception so log it, and
            // skip outputting the attribute
            error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        } catch (RuntimeMBeanException e) {
            // The code inside the attribute getter threw an exception, so we skip
            // outputting the attribute. We will log the exception in certain cases,
            // but suppress the log message in others. See OPSAPS-5449 for more info.
            if (!(e.getCause() instanceof UnsupportedOperationException)) {
                error("getting attribute " + attName + " of " + oname + " threw " +
                        "an exception", e);
            }
            return;
        } catch (RuntimeException e) {
            //For some reason even with an MBeanException available to them Runtime exceptions
            //can still find their way through, so treat them the same as MBeanException
            error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        } catch (ReflectionException e) {
            //This happens when the code inside the JMX bean (setter?? from the java docs)
            //threw an exception, so log it and skip outputting the attribute
            error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        } catch (InstanceNotFoundException e) {
            //Ignored the mbean itself was not found, which should never happen because we
            //just accessed it (perhaps something unregistered in-between) but if this
            //happens just don't output the attribute.
            return;
        }

        writeAttribute(jg, attName, value);
    }

    private static void error(String string, Throwable e) {
        System.err.print(string);
        e.printStackTrace(System.err); 
    }



    private static void writeAttribute(JsonGenerator jg, String attName, Object value) throws IOException {
        jg.writeFieldName(attName);
        writeObject(jg, value);
    }

    private static void writeObject(JsonGenerator jg, Object value) throws IOException {
        if(value == null) {
            jg.writeNull();
        } else {
            Class<?> c = value.getClass();
            if (c.isArray()) {
                jg.writeStartArray();
                int len = Array.getLength(value);
                for (int j = 0; j < len; j++) {
                    Object item = Array.get(value, j);
                    writeObject(jg, item);
                }
                jg.writeEndArray();
            } else if(value instanceof Number) {
                Number n = (Number)value;
                jg.writeNumber(n.toString());
            } else if(value instanceof Boolean) {
                Boolean b = (Boolean)value;
                jg.writeBoolean(b);
            } else if(value instanceof CompositeData) {
                CompositeData cds = (CompositeData)value;
                CompositeType comp = cds.getCompositeType();
                Set<String> keys = comp.keySet();
                jg.writeStartObject();
                for(String key: keys) {
                    writeAttribute(jg, key, cds.get(key));
                }
                jg.writeEndObject();
            } else if(value instanceof TabularData) {
                TabularData tds = (TabularData)value;
                jg.writeStartArray();
                for(Object entry : tds.values()) {
                    writeObject(jg, entry);
                }
                jg.writeEndArray();
            } else {
                jg.writeString(value.toString());
            }
        }
    }
}