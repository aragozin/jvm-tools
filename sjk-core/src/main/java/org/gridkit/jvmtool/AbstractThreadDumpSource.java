package org.gridkit.jvmtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cmd.AntPathMatcher;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.event.ChainedEventReader;
import org.gridkit.jvmtool.event.ErrorHandler;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventDumpParser;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.ShieldedEventReader;
import org.gridkit.jvmtool.event.SimpleErrorEvent;
import org.gridkit.jvmtool.event.SingleEventReader;
import org.gridkit.jvmtool.stacktrace.ThreadEventCodec;
import org.gridkit.jvmtool.stacktrace.analytics.CachingFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.ParserException;
import org.gridkit.jvmtool.stacktrace.analytics.PositionalStackMatcher;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadEventFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.jvmtool.stacktrace.analytics.TimeRangeChecker;
import org.gridkit.jvmtool.stacktrace.analytics.TraceFilterPredicateParser;

import com.beust.jcommander.Parameter;

public abstract class AbstractThreadDumpSource {

    private CommandLauncher host;

    @Parameter(names={"-tf", "--trace-filter"}, required = false, description="Apply filter to traces before processing. Use --ssa-help for more details about filter notation")
    private String traceFilter = null;

    @Parameter(names={"-tt", "--trace-trim"}, required = false, description="Positional filter trim frames to process. Use --ssa-help for more details about filter notation")
    private String traceTrim = null;

    @Parameter(names={"-tn", "--thread-name"}, required = false, description="Thread name filter (Java RegEx syntax)")
    private String threadName = null;

    @Parameter(names={"-tr", "--time-range"}, required = false, description="Time range filter")
    private String timeRange = null;

    private boolean wildcardFileMatching = true;
    
    private TimeZone timeZone;
    
    private List<String> matchedInputFiles;
    
    public AbstractThreadDumpSource(CommandLauncher host) {
        this.host = host;
    }
    
    public void setTimeZone(TimeZone tz) {
        this.timeZone = tz;
    }
    
    public void useWildcards(boolean enabled) {
    	wildcardFileMatching = enabled;
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

    protected abstract List<String> inputFiles();
    
    public List<String> sourceFiles() {
    	if (matchedInputFiles == null) {

    		matchedInputFiles = new ArrayList<String>();
    		
        	if (wildcardFileMatching) {
    	    	List<String> rawInput = inputFiles();
    	
    		    AntPathMatcher matcher = new AntPathMatcher();
    		    matcher.setPathSeparator("/");
    		    
    			
    			for(String f: rawInput) {
    				f = f.replace('\\', '/');
    		        for(File ff: matcher.findFiles(new File("."), f)) {
    		            if (ff.isFile()) {
    		                matchedInputFiles.add(ff.getPath());
    		            }
    		        }
    			}
        	}
        	else {
        		matchedInputFiles.addAll(inputFiles());
        	}

    		if (matchedInputFiles.isEmpty()) {
                host.fail("No input files provided");
            }    		
    	}
    	return matchedInputFiles;
    }
    
    public EventReader<ThreadSnapshotEvent> getUnclassifiedReader() {

        final Iterator<String> it = sourceFiles().iterator();
        ChainedEventReader<Event> reader = new ChainedEventReader<Event>() {

            @Override
            protected EventReader<Event> produceNext() {
                return it.hasNext() ? openReader(it.next()) : null;
            }
        };

        ShieldedEventReader<ThreadSnapshotEvent> shielderReader = new ShieldedEventReader<ThreadSnapshotEvent>(reader, ThreadSnapshotEvent.class, new ErrorHandler() {
            @Override
            public void onException(Exception e) {
                System.err.println("Stream reader error: " + e);
            }
        });

        return shielderReader;
    }    
    
    protected EventReader<Event> openReader(String file) {
        try {
            return ThreadEventCodec.createEventReader(new FileInputStream(file));
        } catch (IOException e) {
        	EventReader<Event> reader = openGenericDump(file);
        	if (reader != null) {
        		return reader;
        	}
        	else {
        		return new SingleEventReader<Event>(new SimpleErrorEvent(e));
        	}
        }
    }
    
    protected EventReader<Event> openGenericDump(String file) {
		ServiceLoader<EventDumpParser> loader = ServiceLoader.load(EventDumpParser.class);
		FileSource fs = new FileSource(file);
		for(EventDumpParser edp: loader) {
			try {
				EventReader<Event> r = edp.open(fs);
				if (r != null) {
					return r;
				}
			}
			catch(Exception e) {
				// ignore
			}
			catch(NoClassDefFoundError ee) {
				ee.printStackTrace();
				// ignore
			}
		}
		return null;
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
    
    private static class FileSource implements EventDumpParser.InputStreamSource {

    	final String file;
    	InputStream is;
    	
		public FileSource(String file) {
			this.file = file;
		}

		@Override
		public InputStream open() throws IOException {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					// ignore
				}
			}
			return is = new FileInputStream(file);
		}
    }
}
