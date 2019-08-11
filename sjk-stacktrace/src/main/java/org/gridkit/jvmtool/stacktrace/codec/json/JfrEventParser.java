package org.gridkit.jvmtool.stacktrace.codec.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.spi.parsers.JsonEventSource;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonEntityHandler;
import org.gridkit.jvmtool.stacktrace.codec.json.JsonStreamHandler.JsonObjectHandler;

public class JfrEventParser implements JsonEventAdapter {

    private JsonStreamHandler streamHandler = new JsonStreamHandler();
    private ObjectHandlerProxy handlerProxy = new ObjectHandlerProxy();
    private CommonEvent nextEvent;

    @Override
    public CommonEvent parseNextEvent(JsonEventSource source) throws IOException {
        nextEvent = null;
        while(true) {
            streamHandler.reset(handlerProxy);
            if (!source.readNext(streamHandler)) {
                return null;
            }
            if (!streamHandler.isAcomplished()) {
                throw new IOException("Malformed JSON input");
            }
            if (nextEvent != null) {
                return nextEvent;
            }
        }
    }

    public void push(CommonEvent nextEvent) {
        if (this.nextEvent != null) {
            throw new IllegalStateException("Event is already slotted");
        }
        this.nextEvent = nextEvent;
    }

    protected JsonObjectHandler createEventHandler(String typeId) {
        if (JfrExecutionSampleHandler.TYPE_ID.equals(typeId)) {
            return new JfrExecutionSampleHandler(this);
        }
        else if (JfrNativeMethodSampleHandler.TYPE_ID.equals(typeId)) {
            return new JfrNativeMethodSampleHandler(this);
        }
        else if (JfrJavaExceptionThrowHandler.TYPE_ID.equals(typeId)) {
            return new JfrJavaExceptionThrowHandler(this);
        }
        else if (JfrObjectAllocationInNewTLAB.TYPE_ID.equals(typeId)) {
            return new JfrObjectAllocationInNewTLAB(this);
        }
        else {
            return JsonStreamHandler.NULL_HANDLER;
        }
    }

    private class ObjectHandlerProxy implements JsonObjectHandler {

        private Map<String, JsonObjectHandler> handlerCache = new HashMap<String, JsonObjectHandler>();
        private JsonObjectHandler delegate;

        @Override
        public void onScalarFieldValue(String fieldName, Object val) {
            if (delegate != null) {
                delegate.onScalarFieldValue(fieldName, val);
            }
            else {
                if ("eventType".equals(fieldName)) {
                    String type = String.valueOf(val);
                    JsonObjectHandler h = handlerCache.get(type);
                    if (h == null) {
                        h = createEventHandler((String)val);
                        handlerCache.put(type, h);
                    }
                    delegate = h;
                }
                else {
                    throw new IllegalStateException("Unexpected field '" + fieldName + "', 'eventType' is expected");
                }
            }
        }

        @Override
        public JsonEntityHandler onEntityField(String fieldName) {
            if (delegate != null) {
                return delegate.onEntityField(fieldName);
            }
            else {
                throw new IllegalStateException("Unexpected field '" + fieldName + "', 'eventType' is expected");
            }
        }

        @Override
        public void onObjectComplete() {
            if (delegate != null) {
                delegate.onObjectComplete();
            }
        }
    }
}
