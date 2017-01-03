package org.gridkit.jvmtool.stacktrace;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class DataBuffer implements DataOutput {

    private byte[] buffer = new byte[512];
    private int size = 0;
    private java.io.DataOutputStream dos;

    public DataBuffer() {
        dos = new java.io.DataOutputStream(new StreamStub());
    }

    public void unloadTo(DataOutput out) throws IOException {
        out.write(buffer, 0, size);
        size = 0;
    }

    @Override
    public void write(int b) throws IOException {
        dos.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        dos.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        dos.write(b, off, len);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        dos.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        dos.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        dos.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        dos.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        dos.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        dos.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        dos.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        dos.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        dos.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        dos.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        dos.writeUTF(s);
    }

    private class StreamStub extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            ensureSize(size + 1);
            buffer[size] = (byte) b;
            size++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ensureSize(size + len);
            System.arraycopy(b, off, buffer, size, len);
            size += len;
        }

        private void ensureSize(int size) {
            while (buffer.length < size) {
                buffer = Arrays.copyOf(buffer, 2 * buffer.length);
            }
        }
    }
}
