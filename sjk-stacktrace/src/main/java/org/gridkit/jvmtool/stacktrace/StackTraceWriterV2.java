package org.gridkit.jvmtool.stacktrace;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

class StackTraceWriterV2 implements StackTraceWriter {

    private DataOutputStream dos;
    private Map<String, Integer> stringDic = new HashMap<String, Integer>();
    private Map<StackTraceElement, Integer> frameDic = new HashMap<StackTraceElement, Integer>();
    private RotatingStringDictionary dynDic = new RotatingStringDictionary(512);
    private List<String> counterKeys = new ArrayList<String>();

    public StackTraceWriterV2(OutputStream os) throws IOException {
        os.write(StackTraceCodec.MAGIC2);
        DeflaterOutputStream def = new DeflaterOutputStream(os);
        this.dos = new DataOutputStream(def);
    }

    @Override
    public void write(ThreadSnapshot snap) throws IOException {
        for(StackFrame ste: snap.stackTrace()) {
            intern(ste.toStackTraceElement());
        }
        for(String ckey: snap.counters()) {
            ensureCounter(ckey);
        }
        int threadNameRef = 0;
        if (snap.threadName() != null) {
            threadNameRef = internDyn(snap.threadName());
        }
        dos.writeByte(StackTraceCodec.TAG_EVENT);
        StackTraceCodec.writeVarLong(dos, snap.threadId());
        StackTraceCodec.writeVarInt(dos, threadNameRef);
        StackTraceCodec.writeTimestamp(dos, snap.timestamp());
        writeState(snap.threadState());
        writeCounters(snap.counters());
        writeTrace(snap.stackTrace());
    }

    private void writeCounters(CounterCollection counters) throws IOException {
        int n = 0;
        while(n < counterKeys.size()) {
            byte mask = 0;
            for(int i = 0; i < 8; ++i) {
                if (n + i >= counterKeys.size()) {
                    break;
                }
                long val = counters.getValue(counterKeys.get(i));
                if (val >= 0) {
                    mask |= 1 << i;
                }
            }
            dos.writeByte(mask);
            n += 8;
        }
        for(String key: counterKeys) {
            long val = counters.getValue(key);
            if (val >= 0) {
                StackTraceCodec.writeVarLong(dos, val);
            }
        }
    }

    private void writeState(State state) throws IOException {
        StackTraceCodec.writeVarInt(dos, state == null ? 0 : (state.ordinal() + 1));
    }

    private void writeTrace(StackFrameList trace) throws IOException {
        int n = 0;
        for(@SuppressWarnings("unused") StackFrame sf: trace) {
            ++n;
        }
        StackTraceCodec.writeVarInt(dos, n);
        for(StackFrame ste: trace) {
            StackTraceCodec.writeVarInt(dos, intern(ste.toStackTraceElement()));
        }
    }

    private int intern(StackTraceElement ste) throws IOException {
        if (!frameDic.containsKey(ste)) {
            String pkg = ste.getClassName();
            int c = pkg.lastIndexOf('.');
            String cn = c < 0 ? pkg : pkg.substring(c + 1);
            pkg = c < 0 ? null : pkg.substring(0, c);
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
            dos.writeByte(StackTraceCodec.TAG_FRAME);
            StackTraceCodec.writeVarInt(dos, npkg);
            StackTraceCodec.writeVarInt(dos, ncn);
            StackTraceCodec.writeVarInt(dos, nmtd);
            StackTraceCodec.writeVarInt(dos, nfile);
            StackTraceCodec.writeVarInt(dos, line);
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
            dos.write(StackTraceCodec.TAG_STRING);
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
            dos.write(StackTraceCodec.TAG_DYN_STRING);
            StackTraceCodec.writeVarInt(dos, n + 1);
            dos.writeUTF(str);
        }
        n = n + 1; // leave zero to be null
        return n;
    }

    private void ensureCounter(String key) throws IOException {
        int n = counterKeys.indexOf(key);
        if (n < 0) {
            n = counterKeys.size();
            counterKeys.add(key);
            dos.write(StackTraceCodec.TAG_COUNTER);
            dos.writeUTF(key);
        }
    }

    @Override
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
