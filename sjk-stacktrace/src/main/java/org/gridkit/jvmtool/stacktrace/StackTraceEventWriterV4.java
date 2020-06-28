package org.gridkit.jvmtool.stacktrace;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.codec.stacktrace.ThreadTraceEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.MultiCounterEvent;
import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.event.TagCollection;
import org.gridkit.jvmtool.event.TaggedEvent;
import org.gridkit.jvmtool.event.TimestampedEvent;
import org.gridkit.jvmtool.event.UniversalEventWriter;
import org.gridkit.jvmtool.jvmevents.JvmEvents;

class StackTraceEventWriterV4 implements UniversalEventWriter {

    private DataOutputStream dos;
    private Map<String, Integer> stringDic = new HashMap<String, Integer>();
    private RotatingStringDictionary dynDic = new RotatingStringDictionary(512);
    {
        for(String s: StackTraceCodec.PRESET_TAG_KEY_V4) {
            stringDic.put(s, stringDic.size() + 1);
        }
        for(String s: StackTraceCodec.PRESET_TAG_TAG_V4) {
            dynDic.intern(s);
        }
    }
    private TagDictionary tagSetDic = new TagDictionary(4 << 10);
    private TagDictionary counterSetDic = new TagDictionary(512);
    private Map<StackFrame, Integer> frameDic = new HashMap<StackFrame, Integer>();

    private TagEncoder encoder = new TagEncoder();
    private TagEncoder counterTagEncoder = new CounterTagEncoder();

    // event encoding details
    private SimpleTagCollection tagBuilder = new SimpleTagCollection();
    private SimpleTagCollection counterBuilder = new SimpleTagCollection();
    private int counterSetRef;

    private ThreadSnapshotEventPojo eventBuf = new ThreadSnapshotEventPojo();

    public StackTraceEventWriterV4(OutputStream os) throws IOException {
        // Magic is written by factory class
        DeflaterOutputStream def = new DeflaterOutputStream(os);
        this.dos = new DataOutputStream(new BufferedOutputStream(def, 32 << 10));
    }

    @Override
    public synchronized void store(Event event) throws IOException {
        try {
            if (event instanceof Error) {
                throw new IllegalArgumentException();
            }
            else if (event instanceof ThreadSnapshotEvent) {
                // enrich tags
                copyToBuf((ThreadSnapshotEvent) event);
                storeThreadEvent(eventBuf);
            }
            else if (event instanceof ThreadTraceEvent) {
                storeThreadEvent((ThreadTraceEvent) event);
            }
            else {
                storeCommonEvent(event);
            }
        }
        catch(IOException e) {
            close();
            throw e;
        }
        catch(RuntimeException e) {
            close();
            throw e;
        }
    }

    private void copyToBuf(ThreadSnapshotEvent event) {
        eventBuf.loadFrom(event);
//        eventBuf.tags().put(JvmEvents.JVM_EVENT_KEY, JvmEvents.EVENT_THREAD_SNAPSHOT);
        if (event.threadId() >= 0) {
            eventBuf.counters().set(JvmEvents.THREAD_ID, event.threadId());
        }
        if (event.threadName() != null) {
            eventBuf.tags().remove(JvmEvents.THREAD_NAME);
            eventBuf.tags().put(JvmEvents.THREAD_NAME, event.threadName());
        }
        if (event.threadState() != null) {
            eventBuf.tags().remove(JvmEvents.THREAD_STATE);
            eventBuf.tags().put(JvmEvents.THREAD_STATE, event.threadState().toString());
        }
    }

    protected void storeThreadEvent(ThreadTraceEvent snap) throws IOException {

        TagCollection stags = (snap instanceof TaggedEvent) ? ((TaggedEvent)snap).tags() : null;
        CounterCollection scc = (snap instanceof MultiCounterEvent) ? ((MultiCounterEvent)snap).counters() : null;

        tagBuilder.clear();
        markTags(); // mark unconditionally

        long timestamp = -1;
        if (snap instanceof TimestampedEvent) {
            markTimestamp();
            timestamp = ((TimestampedEvent) snap).timestamp();
        }

        //TODO empty stack trace is encoded for compatibility with old readers
        if (snap.stackTrace() != null /*&& !snap.stackTrace().isEmpty()*/) {
            markThreadStackTrace();
            for(StackFrame ste: snap.stackTrace()) {
                intern(ste);
            }
        }

        if (scc != null) {
            ensureCounters(scc);
        }

        int tagSetId = ensureTagSet(stags);

        dos.writeByte(StackTraceCodec.TAG_EVENT);
        StackTraceCodec.writeVarInt(dos, tagSetId);

        writeTimestamp(timestamp);
        writeCounters(scc);
        writeTrace(snap.stackTrace());
    }

    protected void storeCommonEvent(Event snap) throws IOException {

        tagBuilder.clear();
        markTags();

        long timestamp = -1;
        if (snap instanceof TimestampedEvent) {
            markTimestamp();
            timestamp = ((TimestampedEvent) snap).timestamp();
        }

        CounterCollection counters = null;
        if (snap instanceof MultiCounterEvent) {
            ensureCounters(((MultiCounterEvent) snap).counters());
            counters = ((MultiCounterEvent) snap).counters();
        }

        int tagSetId;

        if (snap instanceof TaggedEvent) {
            tagSetId = ensureTagSet(((TaggedEvent) snap).tags());
        }
        else {
            tagSetId = ensureTagSet(new SimpleTagCollection());
        }

        dos.writeByte(StackTraceCodec.TAG_EVENT);
        StackTraceCodec.writeVarInt(dos, tagSetId);

        writeTimestamp(timestamp);
        writeCounters(counters);
    }

    private void writeTimestamp(long timestamp) throws IOException {
        if (hasTimestampMark()) {
            StackTraceCodec.writeTimestamp(dos, timestamp);
        }
    }

    private void writeCounters(CounterCollection counters) throws IOException {

        if (hasCountersMark()) {
            StackTraceCodec.writeVarInt(dos, counterSetRef);
            for(String key: counterBuilder) {
                long v = counters.getValue(key);
                StackTraceCodec.writeVarLong(dos, v);
            }
        }
    }

    private void writeTrace(StackFrameList trace) throws IOException {
        if (hasStackTraceMark()) {
            int n = 0;
            for(@SuppressWarnings("unused") StackFrame sf: trace) {
                ++n;
            }
            StackTraceCodec.writeVarInt(dos, n);
            for(StackFrame ste: trace) {
                StackTraceCodec.writeVarInt(dos, intern(ste));
            }
        }
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

    private int intern(StackFrame ste) throws IOException {
        if (!frameDic.containsKey(ste)) {
            String pkg = ste.getClassName();
            int c = pkg.lastIndexOf('.');
            String cn = c < 0 ? pkg : pkg.substring(c + 1);
            pkg = c < 0 ? null : pkg.substring(0, c);
            String mtd = ste.getMethodName();
            String file = ste.getSourceFile();
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

    private int ensureTagSet(TagCollection tags) throws IOException {
        try {
            if (tags != null) {
                for(String key: tags) {
                    if (!StackTraceCodec.TK_PART.equals(key)) {
                        // ignore storage tags
                        for(String tag: tags.tagsFor(key)) {
                            tagBuilder.put(key, tag);
                        }
                    }
                }
            }

            int tagSetId = tagSetDic.intern(tagBuilder, encoder);
            return tagSetId;
        }
        catch(CapsuleException e) {
            throw uncapsuleExcepion(e);
        }
    }

    private void ensureCounters(CounterCollection counters) throws IOException {
        try {
            counterSetRef = -1;
            if (counters != null) {
                Iterator<String> it = counters.iterator();
                if (it.hasNext()) {
                    markCounters();

                    counterBuilder.clear();
                    while(it.hasNext()) {
                        String key = it.next();
                        if (counters.getValue(key) >= 0) {
                            counterBuilder.put(key, "");
                        }
                    }

                    counterSetRef = counterSetDic.intern(counterBuilder, counterTagEncoder);
                    return ;
                }
            }
        }
        catch(CapsuleException e) {
            throw uncapsuleExcepion(e);
        }
    }

    protected IOException uncapsuleExcepion(CapsuleException e) throws IOException {
        if (e.getCause() instanceof IOException) {
            throw (IOException)e.getCause();
        }
        else if (e.getCause() instanceof RuntimeException) {
            throw (RuntimeException)e.getCause();
        }
        else {
            throw new IOException(e.getCause());
        }
    }

    protected void markTimestamp() {
        tagBuilder.put(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_TIMESTAMP);
    }

    protected void markCounters() {
        tagBuilder.put(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_COUNTERS);
    }

    protected void markTags() {
        tagBuilder.put(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_TAGS);
    }

    protected void markThreadStackTrace() {
        tagBuilder.put(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_THREAD_STACK);
    }

    protected boolean hasTimestampMark() {
        return tagBuilder.contains(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_TIMESTAMP);
    }

    protected boolean hasCountersMark() {
        return tagBuilder.contains(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_COUNTERS);
    }

    protected boolean hasStackTraceMark() {
        return tagBuilder.contains(StackTraceCodec.TK_PART, StackTraceCodec.TK_PART_THREAD_STACK);
    }

    @Override
    public synchronized void close() {
        try {
            dos.close();
        } catch (IOException e) {
            // ignore
        }
        stringDic.clear();
        frameDic.clear();
    }

    private class TagEncoder implements TagDictionary.TagSetEncoder {

        DataBuffer buffer = new DataBuffer();
        TagChange pending;

        protected int startTag() {
            return StackTraceCodec.TAG_TAG_SET;
        }

        @Override
        public int cost(String key, String tag) {
            if (tag == null || tag.length() == 0) {
                return 2 + keySize(key);
            }
            else {
                return 3 + keySize(key) + tagSize(tag);
            }
        }

        private int keySize(String key) {
            return stringDic.containsKey(key) ? 2 : 2 + key.length();
        }

        private int tagSize(String tag) {
            return dynDic.contains(tag) ? 2 : 2 + tag.length();
        }

        @Override
        public void startTagSet(int setId, int baseId) {
            try {
                buffer.writeByte(startTag());
                StackTraceCodec.writeVarInt(buffer, setId);
                StackTraceCodec.writeVarInt(buffer, baseId);
//                System.out.print(" [" + setId + "(" + baseId + ")");
            } catch (IOException e) {
                throw new CapsuleException(e);
            }
        }

        public void push(TagChange change) {
            try {
                if (pending != null) {
                    if (    (pending.op == StackTraceCodec.DIC_REMOVE_KEY && change.op == StackTraceCodec.DIC_ADD_TAG)
                          ||(pending.op == StackTraceCodec.DIC_ADD_TAG && change.op == StackTraceCodec.DIC_REMOVE_KEY)) {

                        if (pending.key.equals(change.key)) {
                            // conflate
                            change.op = StackTraceCodec.DIC_SET_KEY;
                            if (pending.tag != null) {
                                change.tag = pending.tag;
                            }
                            pending = null;
                        }
                    }

                    if (pending != null) {
                        pending.encode(buffer);
                    }
                    pending = null;
                }
                if (change.op == StackTraceCodec.DIC_REMOVE_KEY || change.op == StackTraceCodec.DIC_ADD_TAG) {
                    pending = change; // defer
                }
                else {
                    change.encode(buffer);
                }
            } catch (IOException e) {
                throw new CapsuleException(e);
            }
        }

        @Override
        public void append(String key, String tag) {
            if (tag.length() == 0) {
                push(new TagChange(StackTraceCodec.DIC_ADD_KEY, key, null));
            }
            else {
                push(new TagChange(StackTraceCodec.DIC_ADD_TAG, key, tag));
            }
        }

        @Override
        public void remove(String key) {
            push(new TagChange(StackTraceCodec.DIC_REMOVE_KEY, key, null));
        }

        @Override
        public void remove(String key, String tag) {
            push(new TagChange(StackTraceCodec.DIC_REMOVE_TAG, key, tag));
        }

        @Override
        public void finishTag() {
            try {
                if (pending != null) {
                    pending.encode(buffer);
                    pending = null;
                }
                buffer.unloadTo(dos);
                // end of tag delta
                dos.writeByte(0);
//                System.out.println("]");
            } catch (IOException e) {
                throw new CapsuleException(e);
            }
        }
    }

    private class TagChange {

        int op;
        String key;
        String tag;

        public TagChange(int op, String key, String tag) {
            this.op = op;
            this.key = key;
            this.tag = tag;
        }

        public void encode(DataOutput out) throws IOException {
            switch(op) {
                case StackTraceCodec.DIC_ADD_KEY:
                    out.writeByte(StackTraceCodec.DIC_ADD_KEY);
                    StackTraceCodec.writeVarInt(out, intern(key));
//                    System.out.print(" +" + key);
                    break;
                case StackTraceCodec.DIC_ADD_TAG:
                    out.writeByte(StackTraceCodec.DIC_ADD_TAG);
                    StackTraceCodec.writeVarInt(out, intern(key));
                    StackTraceCodec.writeVarInt(out, internDyn(tag));
//                    System.out.print(" +" + key + ":" + tag);
                    break;
                case StackTraceCodec.DIC_SET_KEY:
                    out.writeByte(StackTraceCodec.DIC_SET_KEY);
                    StackTraceCodec.writeVarInt(out, intern(key));
                    StackTraceCodec.writeVarInt(out, internDyn(tag));
//                    System.out.print(" =" + key + ":" + tag);
                    break;
                case StackTraceCodec.DIC_REMOVE_KEY:
                    out.writeByte(StackTraceCodec.DIC_REMOVE_KEY);
                    StackTraceCodec.writeVarInt(out, intern(key));
//                    System.out.print(" -" + key);
                    break;
                case StackTraceCodec.DIC_REMOVE_TAG:
                    out.writeByte(StackTraceCodec.DIC_REMOVE_TAG);
                    StackTraceCodec.writeVarInt(out, intern(key));
                    StackTraceCodec.writeVarInt(out, internDyn(tag));
//                    System.out.print(" -" + key + ":" + tag);
                    break;
                default:
                    throw new IOException("Encoding error");
            }
        }
    }

    private class CounterTagEncoder extends TagEncoder {

        @Override
        protected int startTag() {
            return StackTraceCodec.TAG_COUNTER;
        }
    }


    private static class CapsuleException extends RuntimeException {

        private static final long serialVersionUID = 20161218L;

        public CapsuleException(Throwable cause) {
            super(cause);
        }
    }
}
