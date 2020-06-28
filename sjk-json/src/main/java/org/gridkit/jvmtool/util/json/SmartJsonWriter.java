package org.gridkit.jvmtool.util.json;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.jackson.DefaultPrettyPrinter;
import org.gridkit.jvmtool.jackson.JsonGenerationException;
import org.gridkit.jvmtool.jackson.JsonGenerator;
import org.gridkit.jvmtool.jackson.JsonMiniFactory;
import org.gridkit.jvmtool.jackson.PrettyPrinter;

public class SmartJsonWriter implements JsonStreamWriter {

    final JsonGenerator gen;
    protected Writer writer;
    String lastField = "/";
    List<String> stack = new ArrayList<String>();
    int skipContent = 0;
    boolean skipValue;

    public SmartJsonWriter(Writer writer) {
        this.writer = writer;
        this.gen = JsonMiniFactory.createJsonGenerator(writer);
        gen.setPrettyPrinter(new DefaultPrettyPrinter());
    }

    public void setPrettyPrinter(PrettyPrinter prettyPrinter) {
        gen.setPrettyPrinter(prettyPrinter);
    }

    public void flush() {
        try {
            gen.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    private void push(String frame) {
        stack.add(frame);
        if (skipValue) {
            ++skipContent;
        }
    }

    private void pop() {
        String frame = stack.remove(stack.size() - 1);
        if ("[]".equals(frame)) {
            lastField = frame;
        }
        if (skipContent > 0) {
            --skipContent;
        }
        if (skipContent == 0) {
            skipValue = false;
        }
    }

    private void endValue() {
        if (skipContent == 0) {
            skipValue = false;
        }
    }

    protected void skipNextValue() {
        skipValue = true;
    }

    protected boolean lastFrameIs(String frame) {
        return frame.equals(stack.get(stack.size() - 1));
    }

    protected boolean lastFramesAre(String... frames) {
        if (stack.size() >= frames.length) {
            for(int i = 0; i != frames.length; ++i) {
                if (!stack.get(stack.size() - frames.length + i).equals(frames[i])) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    protected int getStackDepth() {
        return stack.size();
    }

    protected boolean checkFieldWritable(String field) {
        return true;
    }

    @Override
    public void writeStartArray() throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeStartArray();
        }
        push(lastField);
        lastField = "[]";
    }

    @Override
    public void writeEndArray() throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeEndArray();
        }
        pop();
    }

    @Override
    public void writeStartObject() throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeStartObject();
        }
        push(lastField);
        lastField = null;
    }

    @Override
    public void writeEndObject() throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeEndObject();
        }
        pop();
    }

    @Override
    public void writeFieldName(String name) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(name)) {
                gen.writeFieldName(name);
            }
            else {
                skipValue = true;
            }
        }
        lastField = name;
    }

    @Override
    public void writeString(String text) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeString(text);
        }
        endValue();
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeString(text, offset, len);
        }
        endValue();
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
        if (!skipValue) {
            gen.writeNumber(v);
        }
        endValue();
    }

    @Override
    public void writeNumber(long v) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeNumber(v);
        }
        endValue();
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeNumber(v);
        }
        endValue();
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeNumber(d);
        }
        endValue();
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeNumber(f);
        }
        endValue();
    }

    @Override
    public void writeNumber(BigDecimal dec) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeNumber(dec);
        }
        endValue();
    }

    @Override
    public void writeNumber(String encodedValue)
            throws IOException, JsonGenerationException, UnsupportedOperationException {
        if (!skipValue) {
            gen.writeNumber(encodedValue);
        }
        endValue();
    }

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeBoolean(state);
        }
        endValue();
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        if (!skipValue) {
            gen.writeNull();
        }
        endValue();
    }

    @Override
    public void writeStringField(String fieldName, String value) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeStringField(fieldName, value);
            }
        }
    }

    @Override
    public final void writeBooleanField(String fieldName, boolean value) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeBooleanField(fieldName, value);
            }
        }
    }

    @Override
    public final void writeNullField(String fieldName) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeNullField(fieldName);
            }
        }
    }

    @Override
    public final void writeNumberField(String fieldName, int value) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeNumberField(fieldName, value);
            }
        }
    }

    @Override
    public final void writeNumberField(String fieldName, long value) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeNumberField(fieldName, value);
            }
        }
    }

    @Override
    public final void writeNumberField(String fieldName, double value) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeNumberField(fieldName, value);
            }
        }
    }

    @Override
    public final void writeNumberField(String fieldName, float value) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeNumberField(fieldName, value);
            }
        }
    }

    @Override
    public final void writeNumberField(String fieldName, BigDecimal value) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeNumberField(fieldName, value);
            }
        }
    }

    @Override
    public final void writeArrayFieldStart(String fieldName) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeArrayFieldStart(fieldName);
            }
            else {
                skipValue = true;
            }
        }
        push(fieldName);
        lastField = "[]";
    }

    @Override
    public final void writeObjectFieldStart(String fieldName) throws IOException, JsonGenerationException {
        if (!skipValue) {
            if (checkFieldWritable(fieldName)) {
                gen.writeObjectFieldStart(fieldName);
            }
            else {
                skipValue = true;
            }
        }
        push(fieldName);
    }
}
