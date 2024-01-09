package org.gridkit.util.formating;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

public class JsonExporter {

    public static String mbean2json(Map<String, Object> attributes) {

        StringBuilder sb = new StringBuilder();

        mbean2json(sb, attributes);

        return sb.toString();
    }

    public static void mbean2json(StringBuilder sb, Map<String, Object> attributes) {

        writeStartObject(sb);
        for (Map.Entry<String, Object> attr: attributes.entrySet()) {
            writeFieldName(sb, attr.getKey());
            mbean2json(sb, attr.getValue());
        }
        writeEndObject(sb);

    }

    private static void mbean2json(StringBuilder sb, Object value) {
        if(value == null) {
            writeNull(sb);
        } else {
            Class<?> c = value.getClass();
            if (c.isArray()) {
                writeStartArray(sb);
                int len = Array.getLength(value);
                for (int j = 0; j < len; j++) {
                    Object item = Array.get(value, j);
                    if (j != 0) {
                        sb.append(", ");
                    }
                    mbean2json(sb, item);
                }
                writeEndArray(sb);
            } else if(value instanceof Number) {
                Number n = (Number)value;
                writeNumber(sb, n.toString());
            } else if(value instanceof Boolean) {
                Boolean b = (Boolean)value;
                writeBoolean(sb, b);
            } else if(value instanceof CompositeData) {
                CompositeData cds = (CompositeData)value;
                CompositeType comp = cds.getCompositeType();
                Set<String> keys = comp.keySet();
                writeStartObject(sb);
                for(String key: keys) {
                    writeFieldName(sb, key);
                    mbean2json(sb, cds.get(key));
                }
                writeEndObject(sb);
            } else if(value instanceof TabularData) {
                TabularData tds = (TabularData)value;
                writeStartArray(sb);
                boolean first = true;
                for(Object entry : tds.values()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    mbean2json(sb, entry);
                }
                writeEndArray(sb);
            } else {
                writeString(sb, value.toString());
            }
        }
    }

    private static void writeNull(StringBuilder sb) {
        sb.append("null");
    }

    private static void writeNumber(StringBuilder sb, String num) {
        sb.append(num);
    }

    private static void writeBoolean(StringBuilder sb, boolean b) {
        sb.append(b ? "true" : "false");
    }

    private static void writeStartArray(StringBuilder sb) {
        sb.append("[");
    }

    private static void writeEndArray(StringBuilder sb) {
        sb.append("]");
    }

    private static void writeStartObject(StringBuilder sb) {
        sb.append("{");
    }

    private static void writeEndObject(StringBuilder sb) {
        sb.append("}");
    }

    private static void writeFieldName(StringBuilder sb, String fieldName) {
        if (isCommaNeeded(sb)) {
            sb.append(", ");
        }

        writeString(sb, fieldName);
        sb.append(": ");
    }

    private static void writeString(StringBuilder sb, String fieldName) {
        sb.append("\"");
        for (int i = 0; i < fieldName.length(); ++i) {
            char c = fieldName.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        sb.append("\"");
    }

    private static boolean isCommaNeeded(StringBuilder sb) {
        int c = sb.length() - 1;
        while (c >= 0) {
            char lc = sb.charAt(c);
            if (Character.isWhitespace(lc)) {
                c--;
                continue;
            }
            if (lc == '{' || lc == '[') {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }
}
