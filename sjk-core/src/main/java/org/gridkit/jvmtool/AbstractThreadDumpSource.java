/**
 * Copyright 2017 Alexey Ragozin
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.event.ErrorHandler;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.ShieldedEventReader;
import org.gridkit.jvmtool.event.SimpleErrorEvent;
import org.gridkit.jvmtool.event.SingleEventReader;
import org.gridkit.jvmtool.spi.parsers.FileInputStreamSource;
import org.gridkit.jvmtool.spi.parsers.JsonEventDumpHelper;
import org.gridkit.jvmtool.spi.parsers.JsonEventDumpParserFactory;
import org.gridkit.jvmtool.spi.parsers.JsonEventSource;
import org.gridkit.jvmtool.stacktrace.analytics.CachingFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.Calculators;
import org.gridkit.jvmtool.stacktrace.analytics.ParserException;
import org.gridkit.jvmtool.stacktrace.analytics.PositionalStackMatcher;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadEventFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.jvmtool.stacktrace.analytics.TimeRangeChecker;
import org.gridkit.jvmtool.stacktrace.analytics.TraceFilterPredicateParser;
import org.gridkit.jvmtool.stacktrace.analytics.WeigthCalculator;
import org.gridkit.jvmtool.stacktrace.codec.json.JfrEventParser;
import org.gridkit.jvmtool.stacktrace.codec.json.JfrEventReader;

import com.beust.jcommander.Parameter;

public abstract class AbstractThreadDumpSource extends AbstractEventDumpSource {

    @Parameter(names={"-tf", "--trace-filter"}, required = false, description="Apply filter to traces before processing. Use --ssa-help for more details about filter notation")
    private String traceFilter = null;

    @Parameter(names={"-tt", "--trace-trim"}, required = false, description="Positional filter trim frames to process. Use --ssa-help for more details about filter notation")
    private String traceTrim = null;

    @Parameter(names={"-tn", "--thread-name"}, required = false, description="Thread name filter (Java RegEx syntax)")
    private String threadName = null;

    @Parameter(names = {"--force-jdk-parser"}, required = false, description="Only for JFR. Forces to use JDK built-in parser. Require Java 11")
    private boolean forceJdkParser = false;

    @Parameter(names = {"--force-mc-parser"}, required = false, description="Only for JFR. Forces to use MC code for parsing.")
    private boolean forceMcParser = false;

    @Parameter(names = {"--jfr-event"}, required = false, description="Only for JFR. Available options: EXEC, ALLOC, THROW")
    private String jfrEventType;

    public AbstractThreadDumpSource(CommandLauncher host) {
        super(host);
    }

    private boolean isJfrRequired() {
        return jfrEventType != null || forceJdkParser || forceMcParser;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected EventReader<Event> openReader(String file) {
        try {
            Map<String, String> options = new HashMap<String, String>();

            if (forceJdkParser && forceMcParser) {
                host.failAndPrintUsage("--force-jdk-parser and --force-mc-parser are mutually exclusive options");
            }
            if (forceJdkParser) {
                options.put(JsonEventDumpParserFactory.OPT_USE_NATIVE_JFR_PARSER, "true");
            }
            if (forceMcParser) {
                options.put(JsonEventDumpParserFactory.OPT_USE_NATIVE_JFR_PARSER, "false");
            }

            if (jfrEventType == null || "EXEC".equals(jfrEventType)) {
                options.put(JsonEventDumpParserFactory.OPT_JFR_EVENT_WHITELIST, "jdk.ExecutionSample,jdk.NativeMethodSample");
            }
            else if ("THROW".equals(jfrEventType)) {
                options.put(JsonEventDumpParserFactory.OPT_JFR_EVENT_WHITELIST, "jdk.JavaExceptionThrow");
            }
            else if ("ALLOC".equals(jfrEventType)) {
                options.put(JsonEventDumpParserFactory.OPT_JFR_EVENT_WHITELIST, "jdk.ObjectAllocationInNewTLAB");
            }

            JsonEventSource jevent = JsonEventDumpHelper.open(new FileInputStreamSource(new File(file)), options);
            JfrEventReader reader = new JfrEventReader(jevent, new JfrEventParser());

            return EventReader.class.cast(reader);

        } catch (IOException e) {
            if (isJfrRequired()) {
                return new SingleEventReader<Event>(new SimpleErrorEvent(e));
            }
        }

        return super.openReader(file);
    }

    public WeigthCalculator getWeightCalculator() {
        // Unfortunately there is no common interpretation for all
        // supported formats.
        // Particularly VisualVM format is not event based.
        // Though merging samples produced by different means doesn't
        // make much sense any more.
        // Any way here we need to decide which WeightCalculator to use for all files

        WeigthCalculator calc = tryVisualVMCalculator();

        // TODO JFR specific calculator

        return calc == null ? Calculators.SAMPLES : calc;

    }

    private WeigthCalculator tryVisualVMCalculator() {

        int vvmCount = 0;
        int nonVvmCount = 0;

        for (String source: sourceFiles()) {
            if (new File(source).isFile()) {
                try {
                    EventReader<?> reader = openReader(source);
                    if (reader != null) {
                        if (reader.getClass().getSimpleName().equals("DirectNpsEventAdapter")) {
                            vvmCount++;
                        }
                        else {
                            nonVvmCount++;
                        }
                        reader.dispose();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        if (vvmCount > 0 && nonVvmCount == 0) {
            return new VisualVMSampleCalculator();
        }

        return null;
    }

    public EventReader<ThreadSnapshotEvent> getFilteredReader() {
        if (traceFilter == null && traceTrim == null && threadName == null && timeRange == null) {
            return getUnclassifiedReader();
        }
        else {
            EventReader<ThreadSnapshotEvent> reader = getUnclassifiedReader();
            if (threadName != null) {
                reader = reader.morph(new ThreadNameFilter(threadName));
            }
            if (timeRange != null) {
                String[] lh = timeRange.split("[-]");
                if (lh.length != 2) {
                    host.fail("Invalid time range '" + timeRange + "'", "Valid format yyyy.MM.dd_HH:mm:ss-yyyy.MM.dd_HH:mm:ss hours and higher parts can be ommited");
                }
                TimeRangeChecker checker = new TimeRangeChecker(lh[0], lh[1], timeZone);
                reader = reader.morph(new TimeFilter(checker));
            }
            try {
                CachingFilterFactory factory = new CachingFilterFactory();
                if (traceFilter != null) {
                    ThreadSnapshotFilter ts = TraceFilterPredicateParser.parseFilter(traceFilter, factory);
                    reader = reader.morph(new ThreadEventFilter(ts));
                }
                if (traceTrim != null) {
                    final PositionalStackMatcher mt = TraceFilterPredicateParser.parsePositionMatcher(traceTrim, factory);
                    reader = reader.morph(new TrimProxy() {

                        @Override
                        public ThreadSnapshotEvent morph(ThreadSnapshotEvent event) {
                            int n = mt.matchNext(event, 0);
                            if (n >= 0) {
                                trimPoint = n;
                                return super.morph(event);
                            }
                            else {
                                return null;
                            }
                        }
                    });
                }
                return reader;
            }
            catch(ParserException e) {
                throw host.fail("Failed to parse trace filter - " + e.getMessage() + " at " + e.getOffset() + " [" + e.getParseText() + "]");
            }
        }
    }

    @Override
    protected abstract List<String> inputFiles();

    public EventReader<ThreadSnapshotEvent> getUnclassifiedReader() {

        EventReader<Event> rawReader = getRawReader();

        ShieldedEventReader<ThreadSnapshotEvent> shielderReader = new ShieldedEventReader<ThreadSnapshotEvent>(rawReader, ThreadSnapshotEvent.class, new ErrorHandler() {
            @Override
            public boolean onException(Exception e) {
                System.err.println("Stream reader error: " + e);
                e.printStackTrace();
                return true;
            }
        });

        return shielderReader;
    }

    static class TimeFilter implements EventMorpher<ThreadSnapshotEvent, ThreadSnapshotEvent> {

        TimeRangeChecker checker;

        public TimeFilter(TimeRangeChecker checker) {
            this.checker = checker;
        }

        @Override
        public ThreadSnapshotEvent morph(ThreadSnapshotEvent event) {
            return checker.evaluate(event.timestamp()) ? event : null;
        }
    }

    static class ThreadNameFilter implements EventMorpher<ThreadSnapshotEvent, ThreadSnapshotEvent> {

        Matcher matcher;

        public ThreadNameFilter(String regex) {
            this.matcher = Pattern.compile(regex).matcher("");
        }

        @Override
        public ThreadSnapshotEvent morph(ThreadSnapshotEvent event) {
            return evaluate(event) ? event : null;
        }

        protected boolean evaluate(ThreadSnapshotEvent event) {
            if (event.threadName() != null) {
                matcher.reset(event.threadName());
                return matcher.matches();
            }
            else {
                return false;
            }
        }
    }

    static class TrimProxy implements EventMorpher<ThreadSnapshotEvent, ThreadSnapshotEvent> {

        protected ThreadSnapshotEventPojo snap = new ThreadSnapshotEventPojo();
        protected int trimPoint = 0;

        public TrimProxy() {
        }

        @Override
        public ThreadSnapshotEvent morph(ThreadSnapshotEvent event) {
            snap.loadFrom(event);
            snap.stackTrace(event.stackTrace().fragment(0, trimPoint));
            return snap;
        }
    }
}
