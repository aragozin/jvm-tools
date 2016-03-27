package org.gridkit.jvmtool;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.stacktrace.AbstractFilteringStackTraceReader;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.ReaderProxy;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.jvmtool.stacktrace.analytics.CachingFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.FilteredStackTraceReader;
import org.gridkit.jvmtool.stacktrace.analytics.ParserException;
import org.gridkit.jvmtool.stacktrace.analytics.PositionalStackMatcher;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.jvmtool.stacktrace.analytics.TimeRangeChecker;
import org.gridkit.jvmtool.stacktrace.analytics.TraceFilterPredicateParser;

import com.beust.jcommander.Parameter;

public class ThreadDumpSource {

    private CommandLauncher host;

    @Parameter(names={"-f", "--file"}, required = false, variableArity=true, description="Path to stack dump file")
    private List<String> files;
    
    @Parameter(names={"-tf", "--trace-filter"}, required = false, description="Apply filter to traces before processing. Use --ssa-help for more details about filter notation")
    private String traceFilter = null;

    @Parameter(names={"-tt", "--trace-trim"}, required = false, description="Positional filter trim frames to process. Use --ssa-help for more details about filter notation")
    private String traceTrim = null;

    @Parameter(names={"-tn", "--thread-name"}, required = false, description="Thread name filter (Java RegEx syntax)")
    private String threadName = null;

    @Parameter(names={"-tr", "--time-range"}, required = false, description="Time range filter")
    private String timeRange = null;

    private TimeZone timeZone;
    
    public ThreadDumpSource(CommandLauncher host) {
        this.host = host;
    }
    
    public void setTimeZone(TimeZone tz) {
        this.timeZone = tz;
    }
    
    public StackTraceReader getFilteredReader() throws IOException {
        if (traceFilter == null && traceTrim == null && threadName == null && timeRange == null) {
            return getUnclassifiedReader();
        }
        else {
            StackTraceReader reader = getUnclassifiedReader();
            if (threadName != null) {
                reader = new ThreadNameFilter(reader, threadName);
            }
            if (timeRange != null) {
                String[] lh = timeRange.split("[-]");
                if (lh.length != 2) {
                    host.fail("Invalid time range '" + timeRange + "'", "Valid format yyyy.MM.dd_HH:mm:ss-yyyy.MM.dd_HH:mm:ss hours and higher parts can be ommited");
                }
                TimeRangeChecker checker = new TimeRangeChecker(lh[0], lh[1], timeZone);
                reader = new TimeFilter(reader, checker);
            }
            try {
                CachingFilterFactory factory = new CachingFilterFactory();
                if (traceFilter != null) {
                    ThreadSnapshotFilter ts = TraceFilterPredicateParser.parseFilter(traceFilter, factory);
                    reader = new FilteredStackTraceReader(ts, reader);
                }
                if (traceTrim != null) {
                    final PositionalStackMatcher mt = TraceFilterPredicateParser.parsePositionMatcher(traceTrim, factory);
                    reader = new TrimProxy(reader) {
                        
                        ReaderProxy proxy = new ReaderProxy(reader);
                        
                        @Override
                        public boolean loadNext() throws IOException {
                            while(super.loadNext()) {
                                int n = mt.matchNext(proxy, 0);
                                if (n >= 0) {
                                    trimPoint = n;
                                    return true;
                                }
                            }
                            return false;
                        }
                    };
                }
                return reader;
            }
            catch(ParserException e) {
                throw host.fail("Failed to parse trace filter - " + e.getMessage() + " at " + e.getOffset() + " [" + e.getParseText() + "]");
            }
        }
    }

    public StackTraceReader getUnclassifiedReader() throws IOException {
        if (files == null) {
            host.fail("No input files provided, used -f option");
        }
        final StackTraceReader reader = StackTraceCodec.newReader(files.toArray(new String[0]));
        return new StackTraceReader.StackTraceReaderDelegate() {
            
            @Override
            protected StackTraceReader getReader() {
                return reader;
            }

            @Override
            public boolean loadNext() throws IOException {
                try {
                    return super.loadNext();
                }
                catch(IOException e) {
                    System.err.println("Dump file read error: " + e.toString());
                    return false;
                }
            }
        };
    }    
    
    static class TimeFilter extends AbstractFilteringStackTraceReader {
        
        StackTraceReader reader;
        TimeRangeChecker checker;
        
        public TimeFilter(StackTraceReader reader, TimeRangeChecker checker) {
            this.reader = reader;
            this.checker = checker;
        }

        @Override
        protected boolean evaluate() {
            return checker.evaluate(getTimestamp());
        }

        @Override
        protected StackTraceReader getReader() {
            return reader;
        }
    }

    static class ThreadNameFilter extends AbstractFilteringStackTraceReader {
        
        StackTraceReader reader;
        Matcher matcher;
        
        public ThreadNameFilter(StackTraceReader reader, String regex) {
            this.reader = reader;
            this.matcher = Pattern.compile(regex).matcher("");
        }
        
        @Override
        protected boolean evaluate() {
            if (getThreadName() != null) {
                matcher.reset(getThreadName());
                return matcher.matches();
            }
            else {
                return false;
            }
        }
        
        @Override
        protected StackTraceReader getReader() {
            return reader;
        }
    }
    
    static class TrimProxy implements StackTraceReader {
        
        protected StackTraceReader reader;
        protected int trimPoint = 0;
        
        public TrimProxy(StackTraceReader reader) {
            this.reader = reader;
        }

        public boolean isLoaded() {
            return reader.isLoaded();
        }

        public long getThreadId() {
            return reader.getThreadId();
        }

        public long getTimestamp() {
            return reader.getTimestamp();
        }

        public String getThreadName() {
            return reader.getThreadName();
        }

        public State getThreadState() {
            return reader.getThreadState();
        }

        public CounterCollection getCounters() {
            return reader.getCounters();
        }

        public StackTraceElement[] getTrace() {
            return Arrays.copyOf(reader.getTrace(), trimPoint);
        }

        public StackFrameList getStackTrace() {
            return reader.getStackTrace().fragment(0, trimPoint);
        }

        public boolean loadNext() throws IOException {
            return reader.loadNext();
        }
    }    
}
