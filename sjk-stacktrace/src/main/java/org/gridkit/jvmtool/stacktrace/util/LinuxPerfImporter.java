package org.gridkit.jvmtool.stacktrace.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameArray;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public class LinuxPerfImporter {

    private static final Comparator<PerfTrace> TRACE_CMP = new Comparator<PerfTrace>() {

        @Override
        public int compare(PerfTrace o1, PerfTrace o2) {
            return Double.compare(o1.timestamp, o2.timestamp);
        };
    };

    private static final Comparator<ThreadStream> THREAD_CMP = new Comparator<ThreadStream>() {

        @Override
        public int compare(ThreadStream o1, ThreadStream o2) {
            return Double.compare(o1.getNextTimestamp(), o2.getNextTimestamp());
        };
    };

    private static final Pattern HEADER_PATTERN = Pattern.compile("(.*) (\\d+) (\\d+\\.\\d+):\\s+(\\d+)\\s+(\\w.+):.*");
    private static final Pattern FRAME_PATTERN = Pattern.compile("\t\\s*([a-f0-9]+) (.*) \\((.*)\\)");

    public static Iterator<ThreadSnapshot> parseAndConvert(Reader reader, String eventFilter, double upscale, long timebase) {
        final Converter cnv = new Converter(eventFilter, upscale, timebase);
        Iterator<PerfTrace> it = parse(reader);
        while (it.hasNext()) {
            PerfTrace trace = it.next();
            if (cnv.filter == null) {
                cnv.filter = trace.eventType;
            }
            cnv.add(trace);
        }
        System.out.println("Read " + cnv.total + " '" + cnv.filter + "' perf events from source");

        if (cnv.total == 0) {
            throw new IllegalArgumentException("No matching samples found");
        }

        cnv.rescale();

        return new Iterator<ThreadSnapshot>() {

            ThreadSnapshot next;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = cnv.poll();
                }
                return next != null;
            }

            @Override
            public ThreadSnapshot next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ThreadSnapshot that = next;
                next = null;
                return that;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static StackFrame convertFrame(PerfFrame frame) {

        String classPrefix = "";
        String className = "";
        String methodName = "";
        String fileName = "";
        int lineNumber = -2;

        if (frame.symbol.equals("[unknown]")) {
            if (isKernel(frame)) {
                className = "linux";
                methodName = "kernel";
            } else {
                className = refine(getLib(frame.module));
                methodName = refine(striptOffset(frame.symbol));
            }
        } else {
            if (frame.module.endsWith(".map")) {
                // assume java frame
                String method = stripReturnType(striptOffset(frame.symbol));
                if (method.equals("StubRoutines") || method.equals("Interpreter")) {
                    className = "JVM";
                    methodName = method;
                } else {
                    method = stripParenthesis(method);
                    if (method.indexOf('.') > 0) {
                        return StackFrame.parseFrame(method + "(Unknown source)");
                    } else {
                        return StackFrame.parseFrame("JVM." + method + "(Unknown source)");
                    }
                }
            } else {
                String method = stripReturnType(striptOffset(frame.symbol));
                method = method.replace("::", ".");
                String lib = getLib(frame.module);
                if (lib.length() > 0) {
                    String sf = refine(lib) + "." + method;
                    return StackFrame.parseFrame(sf + "(" + lib + ")");
                } else {
                    return StackFrame.parseFrame(method);
                }
            }
        }

        return new StackFrame(classPrefix, className, methodName, fileName, lineNumber);
    }

    private static String getLib(String module) {
        String lib = module;
        int ch = module.lastIndexOf('/');
        if (ch >= 0) {
            lib = lib.substring(ch + 1);
        }
        if (lib.endsWith(".so")) {
            lib = lib.substring(0, lib.length() - 3);
        }
        return lib;
    }

    private static String stripParenthesis(String method) {
        int ch = method.indexOf('(');
        if (ch > 0) {
            return method.substring(0, ch);
        } else {
            return method;
        }
    }

    private static String stripReturnType(String symbol) {
        if (symbol.startsWith("StubRoutines ")) {
            return "StubRoutines";
        } else {
            int ch = symbol.indexOf(' ');
            if (ch > 0) {
                return symbol.substring(ch + 1);
            } else {
                return symbol;
            }
        }
    }

    private static String striptOffset(String symbol) {
        int ch = symbol.lastIndexOf('+');
        if (ch > 0) {
            return symbol.substring(0, ch);
        } else {
            return symbol;
        }
    }

    private static String refine(String module) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < module.length(); ++i) {
            char ch = module.charAt(i);
            if ("[]()".indexOf(ch) >= 0) {
                continue;
            }
            if (":./- ".indexOf(ch) >= 0) {
                sb.append('_');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static boolean isKernel(PerfFrame frame) {
        return frame.address < 0;
    }

    public static Iterator<PerfTrace> parse(final Reader reader) {

        return new Iterator<PerfTrace>() {

            final BufferedReader lineReader = new BufferedReader(reader);
            PerfTrace next;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = parseNext(lineReader);
                }
                return next != null;
            }

            @Override
            public PerfTrace next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                PerfTrace that = next;
                next = null;
                return that;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            private final PerfTrace parseNext(BufferedReader reader) {

                try {
                    PerfTrace trace = readHeader(reader);
                    if (trace == null) {
                        return null;
                    } else {
                        while(true) {
                            PerfFrame frame = readFrame(reader);
                            if (frame == null) {
                                return trace;
                            } else {
                                trace.stack.add(frame);
                            }
                        }
                    }
                } catch (IOException e) {
                    return null;
                }
            }

            private PerfFrame readFrame(BufferedReader reader) throws IOException {
                while(true) {
                    String line = reader.readLine();
                    if (line == null || line.trim().length() == 0) {
                        return null;
                    }
                    PerfFrame pf = parseFrame(line);
                    if (pf != null) {
                        return pf;
                    }
                }
            }

            private PerfTrace readHeader(BufferedReader reader) throws IOException {
                PerfTrace hdr = new PerfTrace();
                while(true) {
                    String line = reader.readLine();
                    if (line == null) {
                        return null;
                    }
                    Matcher m = HEADER_PATTERN.matcher(line);
                    if (m.matches()) {
                        hdr.threadName = m.group(1);
                        hdr.tid = Long.parseLong(m.group(2));
                        hdr.timestamp = Double.parseDouble(m.group(3));
                        hdr.weight = Long.parseLong(m.group(4));
                        hdr.eventType = m.group(5);
                        return hdr;
                    }
                }
            }
        };
    }

    public static PerfFrame parseFrame(String line) {
        Matcher m = FRAME_PATTERN.matcher(line);
        if (m.matches()) {
            PerfFrame frame = new PerfFrame();
            frame.address = parseHex(m.group(1));
            frame.symbol = m.group(2);
            frame.module = m.group(3);
            return frame;
        } else {
            return null;
        }
    }


    public static class PerfTrace {

        public String threadName;
        public long tid;
        public double timestamp;
        public String eventType;
        public long weight;

        public List<PerfFrame> stack = new ArrayList<PerfFrame>();
        public StackFrameArray trace;

    }

    public static class PerfFrame {

        long address;
        String symbol;
        String module;
    }

    @SuppressWarnings("unused")
    private static String toHex(long hex) {
        if (hex < 0) {
            return Long.toHexString((hex >>> 52) & 0xF) + Long.toHexString(hex & 0x1FFFFFFFl).substring(1);
        } else {
            return Long.toHexString(hex);
        }
    }

    private static long parseHex(String hex) {
        if (hex.length() < 16) {
            return Long.parseLong(hex, 16);
        } else {
            long h1 = Long.parseLong(hex.substring(0, 8), 16);
            long h2 = Long.parseLong(hex.substring(8), 16);
            return (h1 << 32 ) | h2;
        }
    }

    private static class Converter {

        private final Map<StackFrame, StackFrame> frameDic = new HashMap<StackFrame, StackFrame>();
        private final Map<Long, ThreadStream> threads = new HashMap<Long, ThreadStream>();

        private final PriorityQueue<ThreadStream> streams = new PriorityQueue<ThreadStream>(16, THREAD_CMP);

        private long total = 0;

        private String filter;
        private double upscale;
        private long timebase;
        private long sampleStep;

        public Converter(String filter, double upscale, long timebase) {
            this.filter = filter;
            this.upscale = upscale;
            this.timebase = timebase;
        }

        private void add(PerfTrace ptrace) {
            if (filter.equals(ptrace.eventType)) {
                total++;
                convertTrace(ptrace);
                if (!threads.containsKey(ptrace.tid)) {
                    threads.put(ptrace.tid, new ThreadStream(ptrace.tid));
                }

                ThreadStream ts = threads.get(ptrace.tid);
                ts.traces.add(ptrace);
                ts.rollingWeight += ptrace.weight;
            }
        }

        private void rescale() {
            long maxWeight = 0;
            long maxEvents = 0;
            for (ThreadStream ts: threads.values()) {
                maxWeight = Math.max(maxWeight, ts.rollingWeight);
                maxEvents = Math.max(maxEvents, ts.traces.size());
            }
            sampleStep = (long)(((double)maxWeight) / maxEvents / upscale);
            if (sampleStep == 0) {
                throw new IllegalArgumentException("Failed to calculate rescale rate");
            }

            for (ThreadStream ts: threads.values()) {
                ts.rollingWeight = 0;
                Collections.sort(ts.traces, TRACE_CMP);
                streams.add(ts);
                ts.initSeek();
            }
        }

        private ThreadSnapshotEvent poll() {
            if (streams.isEmpty()) {
                return null;
            } else {
                ThreadStream stream = streams.poll();
                PerfTrace trace = stream.getNext(sampleStep);
                ThreadSnapshotEventPojo event = new ThreadSnapshotEventPojo();
                event.stackTrace(trace.trace);
                event.threadName(trace.threadName + " (" + trace.tid + ")");
                event.timestamp(timebase + (long)(trace.timestamp * 1000));
                if (stream.getNextTimestamp() < Double.MAX_VALUE) {
                    streams.add(stream);
                }
                return event;
            }
        }

        private void convertTrace(PerfTrace ptrace) {

            List<StackFrame> trace = new ArrayList<StackFrame>();

            StackFrame kframe = null;
            for (PerfFrame pf: ptrace.stack) {
                if (pf.address == 0) {
                    continue;
                }
                if (isKernel(pf) && trace.isEmpty()) {
                    if (kframe == null) {
                        kframe = convert(pf);
                    }
                } else {
                    if (kframe != null) {
                        trace.add(kframe);
                        kframe = null;
                    }
                    trace.add(convert(pf));
                }
            }

            StackFrameArray sfa = new StackFrameArray(trace);
            ptrace.trace = sfa;
            ptrace.stack = null;
        }

        private StackFrame convert(PerfFrame pf) {
            StackFrame sf = convertFrame(pf);
            if (!frameDic.containsKey(sf)) {
                frameDic.put(sf, sf);
                return sf;
            } else {
                return frameDic.get(sf);
            }
        }
    }

    private static class ThreadStream {

        @SuppressWarnings("unused")
        private long threadId;

        private PerfTrace next;

        private List<PerfTrace> traces = new ArrayList<PerfTrace>();

        private long rollingWeight;

        private long rollingThreshold;

        public ThreadStream(long tid) {
            threadId = tid;
        }

        public void initSeek() {
            next = traces.remove(0);
            rollingWeight += next.weight;
            rollingThreshold = 0;
        }

        public PerfTrace getNext(long sampleStep) {
            PerfTrace result = next;
            rollingThreshold += sampleStep;
            while (rollingWeight <= rollingThreshold) {
                if (traces.isEmpty()) {
                    next = null;
                    break;
                }
                next = traces.remove(0);
                rollingWeight += next.weight;
            }
            return result;
        }

        public double getNextTimestamp() {
            return next == null ? Double.MAX_VALUE : next.timestamp;
        }
    }
}
