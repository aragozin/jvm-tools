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
package org.gridkit.jvmtool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.ThreadCounter;
import org.gridkit.jvmtool.stacktrace.ThreadShapshot;

public class StackTraceCodec {

    static final byte[] MAGIC =  toBytes("TRACEDUMP_1 ");
    static final byte[] MAGIC2 = toBytes("TRACEDUMP_2 ");

    private static byte[] toBytes(String text) {
        try {
            return text.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    static final byte TAG_STRING = 1;  
    static final byte TAG_FRAME = 2;  
    static final byte TAG_TRACE = 3;  
    static final byte TAG_DYN_STRING = 4;
    
    static final long TIME_ANCHOR = 1391255854894l;

    public static StackTraceWriter newWriter(OutputStream os) throws IOException {
        return new StackTraceWriter(os);
    }

    public static StackTraceReader newReader(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        byte[] magic = new byte[MAGIC.length];
        dis.readFully(magic);
        if (Arrays.equals(MAGIC, magic)) {
            return new StackTraceReaderV1(is);
        }
        else if (Arrays.equals(MAGIC2, magic)) {
            return new StackTraceReaderV2(is);
        }
        else {
            throw new IOException("Unknown magic [" + new String(magic) + "]");
        }
    }

    public static StackTraceReader newReader(String... files) throws IOException {
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
                        return newReader(fis);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        };
    }

    abstract static class ChainedStackTraceReader implements StackTraceReader {

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
        public long getCounter(ThreadCounter counter) {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getCounter(counter);
        }

        @Override
        public long getCounter(int counterId) {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getCounter(counterId);
        }

        @Override
        public StackTraceElement[] getTrace() {
            if (current == null) {
                new NoSuchElementException();
            }
            return current.getTrace();
        }

        @Override
        public StackFrame[] getStackTrace() {
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
    
    public static class StackTraceWriter {
        
        private DataOutputStream dos;
        private Map<String, Integer> stringDic = new HashMap<String, Integer>();
        private Map<StackTraceElement, Integer> frameDic = new HashMap<StackTraceElement, Integer>();
        private RotatingStringDictionary dynDic = new RotatingStringDictionary(512);

        public StackTraceWriter(OutputStream os) throws IOException {
            os.write(MAGIC2);
            DeflaterOutputStream def = new DeflaterOutputStream(os);
            this.dos = new DataOutputStream(def);
        }
        
        public void write(ThreadShapshot snap) throws IOException {
            for(StackTraceElement ste: snap.elements) {
                intern(ste);
            }
            int threadNameRef = 0;
            if (snap.threadName != null) {
                threadNameRef = internDyn(snap.threadName);
            }
            dos.writeByte(TAG_TRACE);
            writeVarLong(dos, snap.threadId);
            writeVarInt(dos, threadNameRef);
            writeTimestamp(dos, snap.timestamp);
            writeState(snap.state);
            writeCounters(snap.counters);
            writeTrace(snap.elements);
        }

        private void writeCounters(long[] counters) throws IOException {
            if (counters != null) {
                int bits = 0;
                for(int i = 0; i != 32; ++i) {
                    if (counters[i] > 0) {
                        bits |= (1 << i);
                    }
                }
                writeVarInt(dos, (0xFFFF & bits)); // standard
                writeVarInt(dos, bits >> 16); // custom
                for(int i = 0; i != 32; ++i) {
                    if (counters[i] > 0) {
                        writeVarLong(dos, counters[i]);
                    }
                }
            }
            else {
                writeVarInt(dos, 0); // standard
                writeVarInt(dos, 0); // custom
            }
        }

        private void writeState(State state) throws IOException {
            writeVarInt(dos, state == null ? 0 : (state.ordinal() + 1));
        }

        private void writeTrace(StackTraceElement[] trace) throws IOException {
            writeVarInt(dos, trace.length);
            for(StackTraceElement ste: trace) {
                writeVarInt(dos, intern(ste));
            }
        }

        private int intern(StackTraceElement ste) throws IOException {
            if (!frameDic.containsKey(ste)) {
                String pkg = ste.getClassName();
                int c = pkg.lastIndexOf('.');
                String cn = c < 0 ? pkg : pkg.substring(c + 1);
                pkg = c < 0 ? "" : pkg.substring(0, c);
                String mtd = ste.getMethodName();
                String file = ste.getFileName();
                int line = ste.getLineNumber() + 2;
                if (line < 0) {
                    line = 0;
                }
                int npkg = intern(pkg);
                int ncn = intern(cn);
                int nmtd = intern(mtd);
                int nfile = intern(file);
                dos.writeByte(TAG_FRAME);
                writeVarInt(dos, npkg);
                writeVarInt(dos, ncn);
                writeVarInt(dos, nmtd);
                writeVarInt(dos, nfile);
                writeVarInt(dos, line);
                int n = frameDic.size() + 1;
                frameDic.put(ste, n);
            }
            return frameDic.get(ste);            
        }

        private int intern(String str) throws IOException {
            if (str == null) {
                return 0;
            }
            if (!stringDic.containsKey(str)) {
                dos.write(TAG_STRING);
                dos.writeUTF(str);
                int n = stringDic.size() + 1;
                stringDic.put(str, n);
            }
            return stringDic.get(str);            
        }        
        
        private int internDyn(String str) throws IOException {
            if (str == null) {
                return 0;
            }
            int n = dynDic.intern(str);
            if (n < 0) {
                n = ~n;
                dos.write(TAG_DYN_STRING);
                writeVarInt(dos, n + 1);
                dos.writeUTF(str);
            }
            n = n + 1; // leave zero to be null
            return n;
        }

        public void close() {
            try {
                dos.close();
            } catch (IOException e) {
                // ignore
            }
            stringDic.clear();
            frameDic.clear();
        }
    }    
    
    static class StackTraceReaderV1 implements StackTraceReader {
        
        private DataInputStream dis;
        private List<String> stringDic = new ArrayList<String>();
        private List<StackFrame> frameDic = new ArrayList<StackFrame>();
        private Map<StackFrame, StackTraceElement> frameCache = new HashMap<StackFrame, StackTraceElement>();
        
        private boolean loaded;
        private long threadId;
        private long timestamp;
        private StackFrame[] trace;
        
        public StackTraceReaderV1(InputStream is) {
            this.dis = new DataInputStream(new InflaterInputStream(is));
            stringDic.add(null);
            frameDic.add(null);
            loaded = false;;
        }
        
        @Override
        public boolean isLoaded() {
            return loaded;
        }
        
        @Override
        public long getThreadId() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            return threadId;
        }

        @Override
        public long getTimestamp() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            return timestamp;
        }
        
        @Override
        public StackTraceElement[] getTrace() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            StackTraceElement[] strace = new StackTraceElement[trace.length];
            for(int i = 0; i != strace.length; ++i) {
                StackFrame frame = trace[i];
                StackTraceElement e = frameCache.get(frame);
                if (e == null) {
                    frameCache.put(frame, e = frame.toStackTraceElement());
                }
                strace[i] = e;
            }
            return strace;
        }

        @Override
        public StackFrame[] getStackTrace() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            return trace;            
        }
        
        @Override
        public String getThreadName() {
            return null;
        }

        @Override
        public State getThreadState() {
            return null;
        }

        @Override
        public long getCounter(ThreadCounter counter) {
            if (counter == null) {
                throw new NullPointerException("counter is null");
            }
            return -1;
        }

        @Override
        public long getCounter(int counterId) {
            return -1;
        }

        @Override
        public boolean loadNext() throws IOException {
            loaded = false;
            while(true) {
                int tag = dis.read();
                if (tag < 0) {
                    dis.close();
                    break;
                }
                else if (tag == TAG_STRING) {
                    String str = dis.readUTF();
                    stringDic.add(str);                    
                }
                else if (tag == TAG_FRAME) {
                    StackFrame ste = readStackTraceElement();
                    frameDic.add(ste);
                }
                else if (tag == TAG_TRACE) {
                    threadId = dis.readLong();
                    timestamp = dis.readLong();
                    int len = readVarInt(dis);
                    trace = new StackFrame[len];
                    for(int i = 0; i != len; ++i) {
                        int ref = readVarInt(dis);
                        trace[i] = frameDic.get(ref);
                    }
                    loaded = true;
                    break;
                }
                else {
                    throw new IOException("Data format error");
                }
            }
            return loaded;
        }

        private StackFrame readStackTraceElement() throws IOException {
            int npkg = readVarInt(dis);
            int ncn = readVarInt(dis);
            int nmtd = readVarInt(dis);
            int nfile = readVarInt(dis);
            int line = readVarInt(dis) - 2;
            String cp = stringDic.get(npkg);
            String cn = stringDic.get(ncn);
            String mtd = stringDic.get(nmtd);
            String file = stringDic.get(nfile);
            StackFrame e = new StackFrame(cp, cn, mtd, file, line);
            return e;
        }
    }

    static class StackTraceReaderV2 implements StackTraceReader {

        private DataInputStream dis;
        private List<String> stringDic = new ArrayList<String>();
        private List<StackFrame> frameDic = new ArrayList<StackFrame>();
        private Map<StackFrame, StackTraceElement> frameCache = new HashMap<StackFrame, StackTraceElement>();
        private List<String> dynStringDic = new ArrayList<String>();

        private boolean loaded;
        private long threadId;
        private String threadName;
        private long timestamp;
        private State threadState;
        private long[] counters = new long[32];
        private StackFrame[] trace;

        public StackTraceReaderV2(InputStream is) {
            this.dis = new DataInputStream(new InflaterInputStream(is));
            stringDic.add(null);
            dynStringDic.add(null);
            frameDic.add(null);
            loaded = false;;
        }

        @Override
        public boolean isLoaded() {
            return loaded;
        }

        @Override
        public long getThreadId() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            return threadId;
        }

        @Override
        public long getTimestamp() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            return timestamp;
        }

        @Override
        public StackTraceElement[] getTrace() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            StackTraceElement[] strace = new StackTraceElement[trace.length];
            for(int i = 0; i != strace.length; ++i) {
                StackFrame frame = trace[i];
                StackTraceElement e = frameCache.get(frame);
                if (e == null) {
                    frameCache.put(frame, e = frame.toStackTraceElement());
                }
                strace[i] = e;
            }
            return strace;
        }

        @Override
        public StackFrame[] getStackTrace() {
            if (!isLoaded()) {
                throw new NoSuchElementException();
            }
            return trace;
        }

        @Override
        public String getThreadName() {
            return threadName;
        }

        @Override
        public State getThreadState() {
            return threadState;
        }

        @Override
        public long getCounter(ThreadCounter counter) {
            if (counter == null) {
                throw new NullPointerException("counter is null");
            }
            return counters[counter.ordinal()];
        }

        @Override
        public long getCounter(int counterId) {
            if (counterId < 0 || counterId > 31) {
                throw new IndexOutOfBoundsException("counterId " + counterId + " is out of [0, 31]");
            }
            return counters[counterId];
        }

        @Override
        public boolean loadNext() throws IOException {
            loaded = false;
            while(true) {
                int tag = dis.read();
                if (tag < 0) {
                    dis.close();
                    break;
                }
                else if (tag == TAG_STRING) {
                    String str = dis.readUTF();
                    stringDic.add(str);
                }
                else if (tag == TAG_FRAME) {
                    StackFrame ste = readStackTraceElement();
                    frameDic.add(ste);
                }
                else if (tag == TAG_TRACE) {
                    readTraceInfo();
                    loaded = true;
                    break;
                }
                else if (tag == TAG_DYN_STRING) {
                    int id = readVarInt(dis);
                    String str = dis.readUTF();
                    while(dynStringDic.size() <= id) {
                        dynStringDic.add(null);
                    }
                    dynStringDic.set(id, str);
                }
                else {
                    throw new IOException("Data format error");
                }
            }
            return loaded;
        }

        protected void readTraceInfo() throws IOException {
            threadId = readVarLong(dis);
            threadName = dynStringDic.get(readVarInt(dis));
            timestamp = readTimestamp(dis);
            threadState = readState();
            readCounters();
            readStackTrace();
        }

        protected State readState() throws IOException {
            int n = readVarInt(dis);
            return n == 0 ? null : State.values()[n - 1];
        }

        protected void readCounters() throws IOException {
            int std = readVarInt(dis);
            int cust = readVarInt(dis);
            int mask = std | (cust << 16);
            for(int i = 0; i != counters.length; ++i) {
                if ((mask & (1 << i)) != 0) {
                    counters[i] = readVarLong(dis);
                }
                else {
                    counters[i] = -1;
                }
            }
        }

        protected void readStackTrace() throws IOException {
            int len = readVarInt(dis);
            trace = new StackFrame[len];
            for(int i = 0; i != len; ++i) {
                int ref = readVarInt(dis);
                trace[i] = frameDic.get(ref);
            }
        }

        private StackFrame readStackTraceElement() throws IOException {
            int npkg = readVarInt(dis);
            int ncn = readVarInt(dis);
            int nmtd = readVarInt(dis);
            int nfile = readVarInt(dis);
            int line = readVarInt(dis) - 2;
            String cp = stringDic.get(npkg);
            String cn = stringDic.get(ncn);
            String mtd = stringDic.get(nmtd);
            String file = stringDic.get(nfile);
            StackFrame e = new StackFrame(cp, cn, mtd, file, line);
            return e;
        }
    }

    static int readVarInt(DataInputStream dis) throws IOException {
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
    
    static long readVarLong(DataInputStream dis) throws IOException {
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

    static long readTimestamp(DataInputStream dis) throws IOException {
        return TIME_ANCHOR + readVarLong(dis);
    }

    static void writeVarInt(DataOutputStream dos, int v) throws IOException {
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

    static void writeVarLong(DataOutputStream dos, long v) throws IOException {
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

    static void writeTimestamp(DataOutputStream dos, long epoch) throws IOException {
        writeVarLong(dos, epoch - TIME_ANCHOR);
    }
}
