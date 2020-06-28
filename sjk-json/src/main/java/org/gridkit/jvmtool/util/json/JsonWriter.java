package org.gridkit.jvmtool.util.json;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.gridkit.jvmtool.jackson.DefaultPrettyPrinter;
import org.gridkit.jvmtool.jackson.JsonGenerationException;
import org.gridkit.jvmtool.jackson.JsonGenerator;
import org.gridkit.jvmtool.jackson.JsonMiniFactory;

public class JsonWriter implements JsonStreamWriter {

    Writer writer;
    JsonGenerator gen;

    public JsonWriter(Writer writer) {
        gen = JsonMiniFactory.createJsonGenerator(writer);
        gen.setPrettyPrinter(new DefaultPrettyPrinter());
    }

    public void flush() {
        try {
            gen.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    @Override
    public void writeStartArray() throws IOException, JsonGenerationException {
        gen.writeStartArray();
    }

    @Override
    public void writeEndArray() throws IOException, JsonGenerationException {
        gen.writeEndArray();
    }

    @Override
    public void writeStartObject() throws IOException, JsonGenerationException {
        gen.writeStartObject();
    }

    @Override
    public void writeEndObject() throws IOException, JsonGenerationException {
        gen.writeEndObject();
    }

    @Override
    public void writeFieldName(String name) throws IOException, JsonGenerationException {
        gen.writeFieldName(name);
    }

    @Override
    public void writeString(String text) throws IOException, JsonGenerationException {
        gen.writeString(text);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        gen.writeString(text, offset, len);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException, JsonGenerationException {
        gen.writeRawUTF8String(text, offset, length);
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException, JsonGenerationException {
        gen.writeUTF8String(text, offset, length);
    }

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        gen.writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        gen.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        gen.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        gen.writeRaw(c);
    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        gen.writeRawValue(text);
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
        gen.writeRawValue(text, offset, len);
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        gen.writeRawValue(text, offset, len);
    }

    @Override
    public void writeNumber(int v) throws IOException, JsonGenerationException {
        gen.writeNumber(v);
    }

    @Override
    public void writeNumber(long v) throws IOException, JsonGenerationException {
        gen.writeNumber(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException {
        gen.writeNumber(v);
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException {
        gen.writeNumber(d);
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException {
        gen.writeNumber(f);
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException {
        gen.writeNumber(dec);
    }

    @Override
    public void writeNumber(String encodedValue)
            throws IOException, JsonGenerationException, UnsupportedOperationException {
        gen.writeNumber(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException {
        gen.writeBoolean(state);
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        gen.writeNull();
    }

    @Override
    public void writeStringField(String fieldName, String value) throws IOException, JsonGenerationException {
        gen.writeStringField(fieldName, value);
    }

    @Override
    public final void writeBooleanField(String fieldName, boolean value) throws IOException, JsonGenerationException {
        gen.writeBooleanField(fieldName, value);
    }

    @Override
    public final void writeNullField(String fieldName) throws IOException, JsonGenerationException {
        gen.writeNullField(fieldName);
    }

    @Override
    public final void writeNumberField(String fieldName, int value) throws IOException, JsonGenerationException {
        gen.writeNumberField(fieldName, value);
    }

    @Override
    public final void writeNumberField(String fieldName, long value) throws IOException, JsonGenerationException {
        gen.writeNumberField(fieldName, value);
    }

    @Override
    public final void writeNumberField(String fieldName, double value) throws IOException, JsonGenerationException {
        gen.writeNumberField(fieldName, value);
    }

    @Override
    public final void writeNumberField(String fieldName, float value) throws IOException, JsonGenerationException {
        gen.writeNumberField(fieldName, value);
    }

    @Override
    public final void writeNumberField(String fieldName, BigDecimal value) throws IOException, JsonGenerationException {
        gen.writeNumberField(fieldName, value);
    }

    @Override
    public final void writeArrayFieldStart(String fieldName) throws IOException, JsonGenerationException {
        gen.writeArrayFieldStart(fieldName);
    }

    @Override
    public final void writeObjectFieldStart(String fieldName) throws IOException, JsonGenerationException {
        gen.writeObjectFieldStart(fieldName);
    }
}
