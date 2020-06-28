package org.gridkit.jvmtool.stacktrace;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.InflaterInputStream;

import org.gridkit.jvmtool.codec.stacktrace.ThreadTraceEvent;
import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.event.ErrorEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.MorphingEventReader;
import org.gridkit.jvmtool.event.SimpleErrorEvent;
import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;

class StackTraceEventReaderV4 implements EventReader<Event> {

    private static StackFrameList EMPTY_STACK = new StackFrameArray(new StackFrame[0]);

    private DataInputStream dis;
    private List<String> stringDic = new ArrayList<String>();
    private List<StackFrame> frameDic = new ArrayList<StackFrame>();
    private List<String> dynStringDic = new ArrayList<String>();
    private List<TagCollection> tagDic = new ArrayList<TagCollection>();
    private List<CounterSet> counterDic = new ArrayList<CounterSet>();

    // event details
    private boolean loaded;

    private boolean threadDetails;

//    private long threadId;
//    private String threadName;
    private long timestamp;
//    private State threadState;
    private CounterArray counters;
    private SimpleTagCollection tags = new SimpleTagCollection();
    private StackFrameList trace;

    private ThreadEvent threadEventProxy = new ThreadEvent();
    private DataEvent dataEventProxy = new DataEvent();
    private ErrorEvent errorEventProxy;
    private boolean done;

    public StackTraceEventReaderV4(InputStream is) {
        this.dis = new DataInputStream(new BufferedInputStream(new InflaterInputStream(is), 64 << 10));
        stringDic.add(null);
        stringDic.addAll(Arrays.asList(StackTraceCodec.PRESET_TAG_KEY_V4));
        dynStringDic.add(null);
        dynStringDic.addAll(Arrays.asList(StackTraceCodec.PRESET_TAG_TAG_V4));
        frameDic.add(null);
        tagDic.add(new SimpleTagCollection());
        counterDic.add(new CounterSet(new SimpleTagCollection()));
        loaded = false;
    }

    @Override
    public <M extends Event> EventReader<M> morph(EventMorpher<Event, M> morpher) {
        return MorphingEventReader.morph(this, morpher);
    }

    @Override
    public boolean hasNext() {
        if (!loaded) {
            loadNextEvent();
        }
        return loaded;
    }

    @Override
    public Event next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        loaded = false;
        if (errorEventProxy != null) {
            return errorEventProxy;
        }
        if (threadDetails) {
            return threadEventProxy;
        }
        else {
            return dataEventProxy;
        }
    }

    @Override
    public Event peekNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (errorEventProxy != null) {
            return errorEventProxy;
        }
        if (threadDetails) {
            return threadEventProxy;
        }
        else {
            return dataEventProxy;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Event> iterator() {
        return this;
    }

    @Override
    public void dispose() {
        done = true;
        try {
            dis.close();
        } catch (IOException e) {
            // ignore
        }
    }

    protected void loadNextEvent() {
        if (errorEventProxy != null || done) {
            // error raised, no further events
            return;
        }
        try {
            loadNext();
        }
        catch(Exception e) {
            try {
                dis.close(); // release stream
            } catch (IOException ee) {
                // ignore
            }
            loaded = true;
            errorEventProxy = new SimpleErrorEvent(e);
        }
    }

    protected void loadNext() throws IOException {
        loaded = false;
        while(true) {
            int tag = dis.read();
            if (tag < 0) {
                done = true;
                dis.close();
                break;
            }
            else if (tag == StackTraceCodec.TAG_EVENT) {
                readEvent();
                loaded = true;
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
            else if (tag == StackTraceCodec.TAG_DYN_STRING) {
                int id = StackTraceCodec.readVarInt(dis);
                String str = dis.readUTF();
                ensureSlot(dynStringDic, id);
                dynStringDic.set(id, str);
            }
            else if (tag == StackTraceCodec.TAG_COUNTER) {
                readCounterSet();
            }
            else if (tag == StackTraceCodec.TAG_TAG_SET) {
                readTagSet();
            }
            else {
                throw new IOException("Data format error");
            }
        }
    }

    protected void readEvent() throws IOException {
        threadDetails = false;

        TagCollection tc = tagDic.get(StackTraceCodec.readVarInt(dis));
        tags = new SimpleTagCollection(tc);

        // timestamp
        readEventTimestamp();

        // counters
        readEventCounters();

        // thread stack trace
        readEventStackTrace();

        tags.remove(StackTraceCodec.TK_PART); // wipe encoding tags
    }

    protected void readEventStackTrace() throws IOException {
        if (tags.contains(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_THREAD_STACK)) {
            threadDetails = true;
            readStackTrace();
        }
        else {
            trace = null;
        }
    }

//    protected void readEventThreadDetails() throws IOException {
//        if (tags.contains(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_THREAD_DETAILS)) {
//            threadDetails = true;
//            threadId = StackTraceCodec.readVarLong(dis);
//            threadName = dynStringDic.get(StackTraceCodec.readVarInt(dis));
//            threadState = readState();
//        }
//        else {
//            threadDetails = false;
//        }
//    }
//
    protected void readEventCounters() throws IOException {
        if (tags.contains(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_COUNTERS)) {
            readCounters();
        }
        else {
            counters = counterDic.get(0).counters; // empty counters
        }
    }

    protected void readEventTimestamp() throws IOException {
        if (tags.contains(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_TIMESTAMP)) {
            timestamp = StackTraceCodec.readTimestamp(dis);
        }
        else {
            timestamp = Long.MIN_VALUE;
        }
    }

    protected State readState() throws IOException {
        int n = StackTraceCodec.readVarInt(dis);
        return n == 0 ? null : State.values()[n - 1];
    }

    protected void readTagSet() throws IOException {
        int n = StackTraceCodec.readVarInt(dis);
        int base = StackTraceCodec.readVarInt(dis);

        SimpleTagCollection ts = new SimpleTagCollection(tagDic.get(base));

        readTagSetDelta(ts);

        ensureSlot(tagDic, n);

        tagDic.set(n, ts);
    }

    protected void readCounterSet() throws IOException {
        int n = StackTraceCodec.readVarInt(dis);
        int base = StackTraceCodec.readVarInt(dis);

        SimpleTagCollection ts = new SimpleTagCollection(counterDic.get(base).definition);

        readTagSetDelta(ts);

        ensureSlot(counterDic, n);

        CounterSet cs = new CounterSet(ts);

        counterDic.set(n, cs);
    }

    protected void readTagSetDelta(SimpleTagCollection ts) throws IOException {
        SimpleTagCollection ap = new SimpleTagCollection();
        while(true) {
            int btag = dis.readByte();
            if (btag == 0) {
                break;
            }
            else if (btag == StackTraceCodec.DIC_ADD_TAG) {
                String key = readString();
                String tag = readDynString();
                ap.put(key, tag);
                continue;
            }
            else if (btag == StackTraceCodec.DIC_SET_KEY) {
                String key = readString();
                String tag = readDynString();
                ts.remove(key);
                ap.put(key, tag);
                continue;
            }
            else if (btag == StackTraceCodec.DIC_ADD_KEY) {
                String key = readString();
                ap.put(key, "");
                continue;
            }
            else if (btag == StackTraceCodec.DIC_REMOVE_KEY) {
                String key = readString();
                ts.remove(key);
                continue;
            }
            else if (btag == StackTraceCodec.DIC_REMOVE_TAG) {
                String key = readString();
                String tag = readDynString();
                ts.remove(key, tag);
                continue;
            }
            else {
                throw new IOException("Unexpected tag '" + btag + "'");
            }
        }
        ts.putAll(ap);
    }

    private String readString() throws IOException {
        int id = StackTraceCodec.readVarInt(dis);
        if (id < 0 || id >= stringDic.size()) {
            throw new IOException("Illegal string ref #" + id);
        }
        return stringDic.get(id);
    }

    private String readDynString() throws IOException {
        int id = StackTraceCodec.readVarInt(dis);
        if (id < 0 || id >= dynStringDic.size()) {
            throw new IOException("Illegal string ref #" + id);
        }
        return dynStringDic.get(id);
    }

    protected void readCounters() throws IOException {
        int csid = StackTraceCodec.readVarInt(dis);

        CounterSet cs = counterDic.get(csid);
        for(int i = 0; i != cs.values.length; ++i) {
            cs.values[i] = StackTraceCodec.readVarLong(dis);
        }

        counters = cs.counters;
    }

    protected void readStackTrace() throws IOException {
        int len = StackTraceCodec.readVarInt(dis);
        if (len == 0) {
            trace = EMPTY_STACK;
            return;
        }
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

    private void ensureSlot(List<?> list, int n) {
        while(list.size() < n + 1) {
            list.add(null);
        }
    }

    private class DataEvent implements CommonEvent {

        @Override
        public long timestamp() {
            return timestamp;
        }

        @Override
        public CounterCollection counters() {
            return counters;
        }

        @Override
        public TagCollection tags() {
            return tags;
        }
    }

    private class ThreadEvent extends DataEvent implements ThreadTraceEvent {

//        long threadId;
//        String threadName;
//        State threadState;
//
//        void initThreadDetails() {
//            threadId = counters().getValue(JvmEvents.THREAD_ID);
//            if (threadId < 0) {
//                threadId = -1;
//            }
//            threadName = tags().firstTagFor(JvmEvents.THREAD_NAME);
//            threadState = null;
//            String state = tags().firstTagFor(JvmEvents.THREAD_STATE);
//            if (state != null) {
//                try {
//                    threadState = State.valueOf(state);
//                }
//                catch(Exception e) {
//                    // ignore
//                }
//            }
//        }
//
//        @Override
//        public long threadId() {
//            return threadId;
//        }
//
//        @Override
//        public String threadName() {
//            return threadName;
//        }
//
//        @Override
//        public State threadState() {
//            return threadState;
//        }

        @Override
        public StackFrameList stackTrace() {
            return trace;
        }
    }

    private static class CounterSet {

        TagCollection definition;
        String[] names;
        long[] values;
        CounterArray counters;

        public CounterSet(TagCollection tags) {
            this.definition = tags;
            List<String> n = new ArrayList<String>();
            for(String key: tags) {
                n.add(key);
            }
            names = n.toArray(new String[n.size()]);
            values = new long[names.length];
            counters = new CounterArray(names, values);
        }
    }
}
