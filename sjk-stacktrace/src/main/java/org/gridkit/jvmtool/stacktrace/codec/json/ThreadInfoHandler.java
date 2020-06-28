package org.gridkit.jvmtool.stacktrace.codec.json;

import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonEntityHandler;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonObjectHandler;

abstract class ThreadInfoHandler implements JsonObjectHandler {

    protected String javaName;
    protected long osThreadId;
    protected long javaThreadId;

    @Override
    public void onScalarFieldValue(String fieldName, Object val) {
        if (val != null) {
            if ("javaName".equals(fieldName)) {
                javaName = val == null ? null : String.valueOf(val);
            }
            else if ("osThreadId".equals(fieldName)) {
                if (val instanceof Number) {
                    osThreadId = ((Number) val).longValue();
                }
            }
            else if ("javaThreadId".equals(fieldName)) {
                Long id = JfrHelper.asLong(val);
                if (id != null) {
                    javaThreadId = id;
                }
            }
        }
    }

    @Override
    public JsonEntityHandler onEntityField(String fieldName) {
        return JsonStreamHandler.NULL_HANDLER;
    }

    @Override
    public void onObjectComplete() {
        onComplete();
        javaName = null;
        osThreadId = -1;
        javaThreadId = -1;
    }

    protected abstract void onComplete();
}
