package org.gridkit.jvmtool.stacktrace.codec.json;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.event.GenericEvent;
import org.gridkit.jvmtool.jvmevents.JvmEvents;
import org.gridkit.jvmtool.stacktrace.StackFrameArray;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonEntityHandler;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonObjectHandler;

class JfrNativeMethodSampleHandler implements JsonObjectHandler {

    public static final String TYPE_ID = "jdk.NativeMethodSample";

    private final JfrEventParser parser;
    private GenericEvent event = new GenericEvent();
    private StackFrameList threadTrace;

    private ThreadInfoHandler threadInfo = new ThreadInfoHandler() {

        @Override
        protected void onComplete() {
            if (this.javaName != null) {
                event.tags().put(JvmEvents.THREAD_NAME, this.javaName);
            }
            if (this.javaThreadId > 0) {
                event.counters().set(JvmEvents.THREAD_ID, this.javaThreadId);
            }
        }
    };

    private StackTraceHandler stackTrace = new StackTraceHandler() {

        @Override
        protected void onComplete() {
            threadTrace = new StackFrameArray(frames);
        }
    };

    public JfrNativeMethodSampleHandler(JfrEventParser parser) {
        this.parser = parser;
    }

    @Override
    public void onScalarFieldValue(String fieldName, Object val) {
        if ("startTime".equals(fieldName)) {
            JfrHelper.setTimestamp(event, val);
        }
        else if ("state".equals(fieldName)) {
            JfrHelper.setThreadState(event, val);
        }
    }

    @Override
    public JsonEntityHandler onEntityField(String fieldName) {
        if ("sampledThread".equals(fieldName)) {
            return threadInfo;
        }
        else if ("stackTrace".equals(fieldName)) {
            return stackTrace;
        }
        else {
            return JsonStreamHandler.NULL_HANDLER;
        }
    }

    @Override
    public void onObjectComplete() {
        event.tags().put("jfr.typeId", TYPE_ID);
        ThreadSnapshotEventPojo cevent = new ThreadSnapshotEventPojo();
        cevent.loadFromRawEvent(event);
        cevent.stackTrace(threadTrace);
        parser.push(cevent);
        event = new GenericEvent();
        threadTrace = null;
    }
}
