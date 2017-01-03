/**
 * Copyright 2014 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.stacktrace;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.gridkit.jvmtool.jvmevents.JvmEvents;

public class StackTraceCodec {

    static final byte[] MAGIC =  toBytes("TRACEDUMP_1 ");
    static final byte[] MAGIC2 = toBytes("TRACEDUMP_2 ");
    //static final byte[] MAGIC3 = toBytes("TRACEDUMP_3 ");
    static final byte[] MAGIC4 = toBytes("EVENTDUMP_1 ");

    static final String TK_PART = "(stored-parts)";
    static final String TK_PART_TIMESTAMP = "timestamp";
    static final String TK_PART_THREAD_DETAILS = "thread-details";
    static final String TK_PART_THREAD_STACK = "thread-stack";
    static final String TK_PART_TAGS = "tags";
    static final String TK_PART_COUNTERS = "counters";

    private static byte[] toBytes(String text) {
        try {
            return text.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static final byte TAG_STRING = 1;
    static final byte TAG_FRAME = 2;
    static final byte TAG_EVENT = 3;
    static final byte TAG_DYN_STRING = 4;
    static final byte TAG_COUNTER = 5;
    static final byte TAG_TAG_SET = 6;

    static final long TIME_ANCHOR = 1391255854894l;

    static final byte DIC_ADD_TAG = 1;
    static final byte DIC_ADD_KEY = 2;
    static final byte DIC_SET_KEY = 3;
    static final byte DIC_REMOVE_KEY = 4;
    static final byte DIC_REMOVE_TAG = 5;

    static final String[] PRESET_TAG_KEY_V4 = new String[] {
            "(stored-parts)",
            JvmEvents.JVM_EVENT_KEY,
            JvmEvents.THREAD_ID,
            JvmEvents.THREAD_NAME,
            JvmEvents.THREAD_STATE,
            ThreadCounters.CPU_TIME_MS,
            ThreadCounters.USER_TIME_MS,
            ThreadCounters.ALLOCATED_BYTES,
            ThreadCounters.WAIT_COUNTER,
            ThreadCounters.WAIT_TIME_MS,
            ThreadCounters.BLOCKED_COUNTER,
            ThreadCounters.BLOCKED_TIME_MS,
            JvmEvents.GC_NAME,
            JvmEvents.GC_MEMORY_SPACES,
            JvmEvents.GC_COUNT,
            JvmEvents.GC_TOTAL_TIME_MS,

    };

    static final String[] PRESET_TAG_TAG_V4 = new String[] {
            "",
            TK_PART_COUNTERS,
            TK_PART_TAGS,
            TK_PART_THREAD_DETAILS,
            TK_PART_THREAD_STACK,
            TK_PART_TIMESTAMP,
            JvmEvents.EVENT_THREAD_SNAPSHOT,
            JvmEvents.EVENT_GC,
            JvmEvents.EVENT_STW
    };

    public static StackTraceWriter newWriter(OutputStream os) throws IOException {
        return new StackTraceWriterV2(os);
    }

    public static StackTraceReader newReader(InputStream is) throws IOException {
        return newReaderInternal(is);
    }

    public static StackTraceReader newReader(String... files) throws IOException {
        return newReaderInternal(files);
    }

    public static StackTraceReader newEventReader(InputStream is) throws IOException {
        return newReaderInternal(is);
    }

    public static StackTraceReader newEventReader(String... files) throws IOException {
        return newReaderInternal(files);
    }

    private static StackTraceReader newReaderInternal(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        byte[] magic = new byte[MAGIC.length];
        dis.readFully(magic);
        if (Arrays.equals(MAGIC, magic)) {
            return new StackTraceReaderV1(is);
        }
        else if (Arrays.equals(MAGIC2, magic)) {
            return new StackTraceReaderV2(is);
        }
        else if (Arrays.equals(MAGIC4, magic)) {
            return new LegacyStackReader(ThreadEventCodec.createEventReader(magic,  is));
        }
        else {
            throw new IOException("Unknown magic [" + new String(magic) + "]");
        }
    }

    private static StackTraceReader newReaderInternal(String... files) throws IOException {
        final List<String> fileList = new ArrayList<String>(Arrays.asList(files));
        return new ChainedStackTraceReader() {

            @Override
            protected StackTraceReader next() {
                while(!fileList.isEmpty()) {
                    String file = fileList.remove(0);
                    File f = new File(file);
                    if (!f.isFile()) {
                        continue;
                    }
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        return newReaderInternal(fis);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        };
    }

    public abstract static class ChainedStackTraceReader implements StackTraceReader {

        private StackTraceReader current;

        protected abstract StackTraceReader next();

        @Override
        public boolean isLoaded() {
            if (current == null) {
                current = next();
            }
            return current != null && current.isLoaded();
        }

        @Override
        public long getThreadId() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getThreadId();
        }

        @Override
        public String getThreadName() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getThreadName();
        }

        @Override
        public long getTimestamp() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getTimestamp();
        }

        @Override
        public State getThreadState() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getThreadState();
        }

        @Override
        public CounterCollection getCounters() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getCounters();
        }

        @Override
        public StackTraceElement[] getTrace() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getTrace();
        }

        @Override
        public StackFrameList getStackTrace() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getStackTrace();
        }

        @Override
        public boolean loadNext() throws IOException {
            if (current == null) {
                current = next();
                if (current == null) {
                    return false;
                }
            }
            if (current.loadNext()) {
                return true;
            }
            else {
                current = null;
                return loadNext();
            }
        }
    }

    static int readVarInt(DataInput dis) throws IOException {
        int b = dis.readByte();
        if ((b & 0x80) == 0) {
            return 0x7F & b;
        }
        else {
            int v = (0x7F & b);
            b = dis.readByte();
            v |= ((0x7F & b) << 7);
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7F & b) << 14);
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0xFF & b) << 21);
            return v;
        }
    }

    static long readVarLong(DataInput dis) throws IOException {
        byte b = dis.readByte();
        if ((b & 0x80) == 0) {
            return 0x7F & b;
        }
        else {
            long v = (0x7F & b);
            b = dis.readByte();
            v |= ((0x7Fl & b) << 7); // byte 2
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 14); // byte 3
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 21); // byte 4
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 28); // byte 5
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 35); // byte 6
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 42); // byte 7
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 49); // byte 8
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0xFFl & b) << 56); // byte 9

            return v;
        }
    }

    static long readTimestamp(DataInput dis) throws IOException {
        return TIME_ANCHOR + readVarLong(dis);
    }

    static void writeVarInt(DataOutput dos, int v) throws IOException {
        if (v < 0) {
            throw new IllegalArgumentException("Out of bounds: " + v);
        }
        int val = v;
        if ((val & 0xFFFFFF80) == 0) {
            dos.write(val);
            return;
        }
        dos.write(0x80 | (0x7F & val));
        val >>= 7;
        if ((val & 0xFFFFFF80) == 0) {
            dos.write(val);
            return;
        }
        dos.write(0x80 | (0x7F & val));
        val >>= 7;
        if ((val & 0xFFFFFF80) == 0) {
            dos.write(val);
            return;
        }
        dos.write(0x80 | (0x7F & val));
        val >>= 7;
        if ((val & 0xFFFFFF00) == 0) {
            dos.write(val);
            return;
        }
        else {
            throw new IllegalArgumentException("Out of bounds: " + v);
        }
    }

    static void writeVarLong(DataOutput dos, long v) throws IOException {
        long val = v;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) {
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 2
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 3
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 4
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 5
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 6
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 7
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 8
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF00l) == 0) { // byte 9
            dos.write((int)(0xFF & val));
            return;
        }
        else {
            throw new IllegalArgumentException("Out of bounds: " + v);
        }
    }

    static void writeTimestamp(DataOutput dos, long epoch) throws IOException {
        writeVarLong(dos, epoch - TIME_ANCHOR);
    }
}
