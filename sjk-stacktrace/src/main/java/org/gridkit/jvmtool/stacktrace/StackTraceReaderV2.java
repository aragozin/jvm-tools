package org.gridkit.jvmtool.stacktrace;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.InflaterInputStream;

class StackTraceReaderV2 implements StackTraceReader {

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
    private String[] counterNames = new String[0];
    private long[] counterValues = new long[0];
    private CounterArray counters = new CounterArray(counterNames, counterValues);
    private StackFrameList trace;

    public StackTraceReaderV2(InputStream is) {
        this.dis = new DataInputStream(new BufferedInputStream(new InflaterInputStream(is), 64 << 10));
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
        StackTraceElement[] strace = new StackTraceElement[trace.depth()];
        for(int i = 0; i != strace.length; ++i) {
            StackFrame frame = trace.frameAt(i);
            StackTraceElement e = frameCache.get(frame);
            if (e == null) {
                frameCache.put(frame, e = frame.toStackTraceElement());
            }
            strace[i] = e;
        }
        return strace;
    }

    @Override
    public StackFrameList getStackTrace() {
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
    public CounterCollection getCounters() {
        return counters;
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
            else if (tag == StackTraceCodec.TAG_STRING) {
                String str = dis.readUTF();
                stringDic.add(str);
            }
            else if (tag == StackTraceCodec.TAG_FRAME) {
                StackFrame ste = readStackTraceElement();
                frameDic.add(ste);
            }
            else if (tag == StackTraceCodec.TAG_EVENT) {
                readTraceInfo();
                loaded = true;
                break;
            }
            else if (tag == StackTraceCodec.TAG_DYN_STRING) {
                int id = StackTraceCodec.readVarInt(dis);
                String str = dis.readUTF();
                while(dynStringDic.size() <= id) {
                    dynStringDic.add(null);
                }
                dynStringDic.set(id, str);
            }
            else if (tag == StackTraceCodec.TAG_COUNTER) {
                String str = dis.readUTF();
                int n = counterNames.length;
                counterNames = Arrays.copyOf(counterNames, n + 1);
                counterValues = Arrays.copyOf(counterValues, n + 1);
                counterNames[n] = str;
                counters = new CounterArray(counterNames, counterValues);
            }
            else {
                throw new IOException("Data format error");
            }
        }
        return loaded;
    }

    protected void readTraceInfo() throws IOException {
        threadId = StackTraceCodec.readVarLong(dis);
        threadName = dynStringDic.get(StackTraceCodec.readVarInt(dis));
        timestamp = StackTraceCodec.readTimestamp(dis);
        threadState = readState();
        readCounters();
        readStackTrace();
    }

    protected State readState() throws IOException {
        int n = StackTraceCodec.readVarInt(dis);
        return n == 0 ? null : State.values()[n - 1];
    }

    protected void readCounters() throws IOException {
        Arrays.fill(counterValues, Long.MIN_VALUE);
        boolean[] mask = new boolean[counterNames.length];
        int n = 0;
        while(n < mask.length) {
            byte b = dis.readByte();
            for(int i = 0; i != 8; ++i) {
                if (n + i < mask.length) {
                    mask[n + i] = (b & 1 << i) != 0;
                }
            }
            n += 8;
        }
        for(int i = 0; i != mask.length; ++i) {
            if (mask[i]) {
                counterValues[i] = StackTraceCodec.readVarLong(dis);
            }
        }
    }

    protected void readStackTrace() throws IOException {
        int len = StackTraceCodec.readVarInt(dis);
        StackFrame[] frames = new StackFrame[len];
        for(int i = 0; i != len; ++i) {
            int ref = StackTraceCodec.readVarInt(dis);
            frames[i] = frameDic.get(ref);
        }
        trace = new StackFrameArray(frames);
    }

    private StackFrame readStackTraceElement() throws IOException {
        int npkg = StackTraceCodec.readVarInt(dis);
        int ncn = StackTraceCodec.readVarInt(dis);
        int nmtd = StackTraceCodec.readVarInt(dis);
        int nfile = StackTraceCodec.readVarInt(dis);
        int line = StackTraceCodec.readVarInt(dis) - 2;
        String cp = stringDic.get(npkg);
        String cn = stringDic.get(ncn);
        String mtd = stringDic.get(nmtd);
        String file = stringDic.get(nfile);
        StackFrame e = new StackFrame(cp, cn, mtd, file, line);
        return e;
    }
}
