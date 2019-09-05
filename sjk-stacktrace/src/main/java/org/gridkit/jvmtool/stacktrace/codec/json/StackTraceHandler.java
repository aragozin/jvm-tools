package org.gridkit.jvmtool.stacktrace.codec.json;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonEntityHandler;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonListHandler;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonObjectHandler;

abstract class StackTraceHandler implements JsonObjectHandler {

    protected boolean truncated = false;
    protected List<StackFrame> frames = new ArrayList<StackFrame>();

    protected JsonListHandler frameListHandler = new JsonListHandler() {

        @Override
        public void onNextValue(Object val) {
            throw new IllegalStateException("Frame object is expected");
        }

        @Override
        public JsonEntityHandler onNextEntity() {
            return frameHandler;
        }

        @Override
        public void onListComplete() {
        }
    };

    protected JsonObjectHandler frameHandler = new JsonObjectHandler() {

        private JsonObjectHandler methodHandler = new JsonObjectHandler() {

            @Override
            public void onScalarFieldValue(String fieldName, Object val) {
                if (val != null) {
                    if ("class".equals(fieldName)) {
                        className = asString(val);
                    }
                    else if ("method".equals(fieldName)) {
                        methodName = asString(val);
                    }
                }
            }

            @Override
            public void onObjectComplete() {
                // do nothing
            }

            @Override
            public JsonEntityHandler onEntityField(String fieldName) {
                return JsonStreamHandler.NULL_HANDLER;
            }
        };

        private String className;
        private String methodName;
        private int line = -1;

        @Override
        public void onScalarFieldValue(String fieldName, Object val) {
            if (val != null) {
                if ("lineNumber".equals(fieldName)) {
                    line = asInt(val);
                    line = line == 0 ? -1 : line;
                }
                else if ("type".equals(fieldName) && "Native".equals(val)) {
                    line = -2;
                }
            }
        }

        @Override
        public void onObjectComplete() {
            StackFrame frame = new StackFrame("", String.valueOf(className), String.valueOf(methodName), line >= 0 ? "java" : null, line);
            frames.add(frame);
            className = null;
            methodName = null;
        }

        @Override
        public JsonEntityHandler onEntityField(String fieldName) {
            if ("method".equals(fieldName)) {
                return methodHandler;
            }
            return JsonStreamHandler.NULL_HANDLER;
        }
    };

    @Override
    public void onScalarFieldValue(String fieldName, Object val) {
        if (val != null) {
            if ("truncated".equals(fieldName)) {
                truncated = asBoolean(val);
            }
        }
    }

    @Override
    public JsonEntityHandler onEntityField(String fieldName) {
        if ("frames".equals(fieldName)) {
            return frameListHandler;
        }
        else {
            return JsonStreamHandler.NULL_HANDLER;
        }
    }

    @Override
    public void onObjectComplete() {
        onComplete();
        truncated = false;
        frames.clear();
    }

    protected abstract void onComplete();

    private static String asString(Object val) {
        return String.valueOf(val);
    }

    private static int asInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        else if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Number expected: " + val);
            }
        }
        else {
            throw new IllegalArgumentException("Number expected: " + val);
        }
    }

    private static boolean asBoolean(Object val) {
        if (val instanceof Boolean) {
            return Boolean.TRUE.equals(val);
        }
        else {
            throw new IllegalArgumentException("Boolean expected: " + val);
        }
    }
}
