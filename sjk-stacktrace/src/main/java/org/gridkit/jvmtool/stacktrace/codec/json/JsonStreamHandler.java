package org.gridkit.jvmtool.stacktrace.codec.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.util.json.JsonStreamWriter;

/**
 * Utility class for parsing JSON event stream.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JsonStreamHandler implements JsonStreamWriter {

    public static boolean TRACE = false;

    public static final JsonNullHandler NULL_HANDLER = new JsonNullHandler();

    private List<JsonEntityHandler> handlerStack = new ArrayList<JsonEntityHandler>();
    private boolean starting;
    private String fieldName;

    public JsonStreamHandler() {
    }

    public void reset(JsonEntityHandler handler) {
        handlerStack.clear();
        push(handler);
        starting = true;
    }

    public boolean isAcomplished() {
        return handlerStack.isEmpty();
    }

    protected JsonEntityHandler top() {
        if (handlerStack.isEmpty()) {
            throw new IllegalStateException("Parsing complete");
        }
        return handlerStack.get(handlerStack.size() - 1);
    }

    protected void push(JsonEntityHandler handler) {
        handlerStack.add(handler);
    }

    protected void pop() {
        handlerStack.remove(handlerStack.size() - 1);
    }

    protected JsonListHandler topListHandler() {
        if (handlerStack.isEmpty()) {
            throw new IllegalStateException("Parsing complete");
        }
        JsonEntityHandler h = handlerStack.get(handlerStack.size() - 1);
        if (h instanceof JsonListHandler) {
            return (JsonListHandler) h;
        }
        else {
            throw new IllegalStateException("Unexpected object content");
        }
    }

    protected JsonObjectHandler topObjectHandler() {
        if (handlerStack.isEmpty()) {
            throw new IllegalStateException("Parsing complete");
        }
        JsonEntityHandler h = handlerStack.get(handlerStack.size() - 1);
        if (h instanceof JsonObjectHandler) {
            return (JsonObjectHandler) h;
        }
        else {
            throw new IllegalStateException("Unexpected list content");
        }
    }

    @Override
    public void writeStartArray() throws IOException {
        if (TRACE) {
            System.out.println("writeStartArray");
        }
        if (starting) {
            starting = false;
            topListHandler(); // type check
        }
        else {
            if (fieldName == null) {
                push(topListHandler().onNextEntity());
            }
            else {
                push(topObjectHandler().onEntityField(fieldName));
            }
        }
        fieldName = null;
    }

    @Override
    public void writeEndArray() throws IOException {
        if (TRACE) {
            System.out.println("writeEndArray");
        }
        topListHandler().onListComplete();
        pop();
    }

    @Override
    public void writeStartObject() throws IOException {
        if (TRACE) {
            System.out.println("writeStartObject");
        }
        if (starting) {
            starting = false;
            topObjectHandler(); // type check
        }
        else {
            if (fieldName == null) {
                push(topListHandler().onNextEntity());
            }
            else {
                push(topObjectHandler().onEntityField(fieldName));
            }
        }
        fieldName = null;
    }

    @Override
    public void writeEndObject() throws IOException {
        if (TRACE) {
            System.out.println("writeEndObject");
        }
        topObjectHandler().onObjectComplete();
        pop();
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        if (TRACE) {
            System.out.println("writeFieldName: " + name);
        }
        topObjectHandler();
        fieldName = name;
    }

    protected void onValue(Object val) {
        if (TRACE) {
            System.out.println("writeValue: " + val);
        }
        if (fieldName == null) {
            topListHandler().onNextValue(val);
        }
        else {
            topObjectHandler().onScalarFieldValue(fieldName, val);
        }
        fieldName = null;
    }

    @Override
    public void writeString(String text) throws IOException {
        onValue(text);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        onValue(new String(text, offset, len));
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(String text) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(char c) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeNumber(int v) throws IOException {
        onValue(v);
    }

    @Override
    public void writeNumber(long v) throws IOException {
        onValue(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        onValue(v);
    }

    @Override
    public void writeNumber(double d) throws IOException {
        onValue(d);
    }

    @Override
    public void writeNumber(float f) throws IOException {
        onValue(f);
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException {
        onValue(dec);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException, UnsupportedOperationException {
        onValue(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        onValue(state);
    }

    @Override
    public void writeNull() throws IOException {
        onValue(null);
    }

    @Override
    public void writeStringField(String fieldName, String value) throws IOException {
        writeFieldName(fieldName);
        writeString(value);
    }

    @Override
    public void writeBooleanField(String fieldName, boolean value) throws IOException {
        writeFieldName(fieldName);
        writeBoolean(value);
    }

    @Override
    public void writeNullField(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeNull();
    }

    @Override
    public void writeNumberField(String fieldName, int value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    @Override
    public void writeNumberField(String fieldName, long value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    @Override
    public void writeNumberField(String fieldName, double value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    @Override
    public void writeNumberField(String fieldName, float value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    @Override
    public void writeNumberField(String fieldName, BigDecimal value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    @Override
    public void writeArrayFieldStart(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeStartArray();
    }

    @Override
    public void writeObjectFieldStart(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeStartObject();
    }

    public interface JsonEntityHandler {

    }

    public interface JsonListHandler extends JsonEntityHandler {

        public JsonEntityHandler onNextEntity();

        public void onNextValue(Object val);

        public void onListComplete();
    }

    public interface JsonObjectHandler extends JsonEntityHandler {

        public void onScalarFieldValue(String fieldName, Object val);

        public JsonEntityHandler onEntityField(String fieldName);

        public void onObjectComplete();
    }

    public static class JsonNullHandler implements JsonListHandler, JsonObjectHandler {

        @Override
        public void onScalarFieldValue(String fieldName, Object val) {
            // do nothing
        }

        @Override
        public JsonEntityHandler onEntityField(String fieldName) {
            return this;
        }

        @Override
        public void onObjectComplete() {
            // do nothing
        }

        @Override
        public JsonEntityHandler onNextEntity() {
            return this;
        }

        @Override
        public void onNextValue(Object val) {
            // do nothing
        }

        @Override
        public void onListComplete() {
            // do nothing
        }
    }
}
