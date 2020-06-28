package org.gridkit.jvmtool.stacktrace;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.InflaterInputStream;

class StackTraceReaderV1 implements StackTraceReader {

    private DataInputStream dis;
    private List<String> stringDic = new ArrayList<String>();
    private List<StackFrame> frameDic = new ArrayList<StackFrame>();
    private Map<StackFrame, StackTraceElement> frameCache = new HashMap<StackFrame, StackTraceElement>();

    private boolean loaded;
    private long threadId;
    private long timestamp;
    private StackFrameList trace;

    public StackTraceReaderV1(InputStream is) {
        this.dis = new DataInputStream(new BufferedInputStream(new InflaterInputStream(is), 64 << 10));
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
        return null;
    }

    @Override
    public State getThreadState() {
        return null;
    }

    @Override
    public CounterCollection getCounters() {
        return CounterArray.EMPTY;
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
                threadId = dis.readLong();
                timestamp = dis.readLong();
                int len = StackTraceCodec.readVarInt(dis);
                StackFrame[] frames = new StackFrame[len];
                for(int i = 0; i != len; ++i) {
                    int ref = StackTraceCodec.readVarInt(dis);
                    frames[i] = frameDic.get(ref);
                }
                trace = new StackFrameArray(frames);
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
