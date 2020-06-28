package org.gridkit.jvmtool.stacktrace.codec.json;

import java.util.concurrent.TimeUnit;

import org.gridkit.jvmtool.event.GenericEvent;
import org.gridkit.jvmtool.jvmevents.JvmEvents;

class JfrHelper {

    private static final long NS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

    public static void setTimestamp(GenericEvent ge, Object val) {
        if (val instanceof Number) {
            ge.timestamp(((Number) val).longValue() / NS_PER_MS);
            return;
        }
        else if (val instanceof String) {
            try {
                long ts = Long.parseLong(val.toString());
                ge.timestamp(ts / NS_PER_MS);
                return;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        throw new IllegalArgumentException("Cannot cast " + val + " to time stamp");
    }

    public static void setThreadState(GenericEvent event, Object val) {
        event.tags().put(JvmEvents.THREAD_STATE, convertThreadState(String.valueOf(val)));
    }

    public static void setAllocationSize(GenericEvent ge, Object val) {
        Long lval = asLong(val);
        if (lval != null) {
            ge.counters().set("jfr.ObjectAllocationInNewTLAB.allocationSize",lval.longValue());
        }
        else {
            throw new IllegalArgumentException("Cannot cast " + val + " to number");
        }
    }

    public static void setTLABSize(GenericEvent ge, Object val) {
        Long lval = asLong(val);
        if (lval != null) {
            ge.counters().set("jfr.ObjectAllocationInNewTLAB.tlabSize",lval.longValue());
        }
        else {
            throw new IllegalArgumentException("Cannot cast " + val + " to number");
        }
    }

    public static Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            }
            catch(NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String convertThreadState(String val) {
        if (val.startsWith("STATE_")) {
            return val.substring("STATE_".length());
        }
        else {
            return val;
        }
    }
}
