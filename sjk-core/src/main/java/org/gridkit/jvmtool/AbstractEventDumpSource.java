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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TimeZone;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cmd.AntPathMatcher;
import org.gridkit.jvmtool.event.ChainedEventReader;
import org.gridkit.jvmtool.event.ErrorHandler;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventDumpParser;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.ShieldedEventReader;
import org.gridkit.jvmtool.event.SimpleErrorEvent;
import org.gridkit.jvmtool.event.SingleEventReader;
import org.gridkit.jvmtool.event.TimestampedEvent;
import org.gridkit.jvmtool.stacktrace.ThreadEventCodec;
import org.gridkit.jvmtool.stacktrace.analytics.TimeRangeChecker;

import com.beust.jcommander.Parameter;

public abstract class AbstractEventDumpSource {

    protected CommandLauncher host;

    @Parameter(names={"-tr", "--time-range"}, required = false, description="Time range filter")
    protected String timeRange = null;

    @Parameter(names={"--parsers-info"}, required = false, description="Print parsers available in classpath")
    protected boolean reportParsers = false;
    
    private boolean wildcardFileMatching = true;
    
    protected TimeZone timeZone;
    
    private List<String> matchedInputFiles;
    
    public AbstractEventDumpSource(CommandLauncher host) {
        this.host = host;
    }
    
    public void setTimeZone(TimeZone tz) {
        this.timeZone = tz;
    }
    
    public void useWildcards(boolean enabled) {
    	wildcardFileMatching = enabled;
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
    
    public EventReader<Event> getFilteredRawReader() {
    	if (timeRange != null) {
	        String[] lh = timeRange.split("[-]");
	        if (lh.length != 2) {
	            host.fail("Invalid time range '" + timeRange + "'", "Valid format yyyy.MM.dd_HH:mm:ss-yyyy.MM.dd_HH:mm:ss hours and higher parts can be ommited");
	        }
	        TimeRangeChecker checker = new TimeRangeChecker(lh[0], lh[1], timeZone);
	    	return getRawReader().morph(new TimeFilter(checker));
    	}
    	else {
    		return getRawReader();
    	}
    }
    
    public EventReader<Event> getRawReader() {

        final Iterator<String> it = sourceFiles().iterator();
        ChainedEventReader<Event> reader = new ChainedEventReader<Event>() {

            @Override
            protected EventReader<Event> produceNext() {
                return it.hasNext() ? openReader(it.next()) : null;
            }
        };

        ShieldedEventReader<Event> shieldedReader = new ShieldedEventReader<Event>(reader, Event.class, new ErrorHandler() {
            @Override
            public boolean onException(Exception e) {
                System.err.println("Stream reader error: " + e);
                return true;
            }
        });


        return shieldedReader;
    }    
    
    protected EventReader<Event> openReader(String file) {
    	if (reportParsers) {
    		printParsers();
    	}
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
    
    private void printParsers() {
    	System.out.println("Available file parsers");
    	for(String parser: ThreadEventCodec.listSupportedFormats()) {
    		System.out.println(" - " + parser);
    	}
    	ServiceLoader<EventDumpParser> loader = ServiceLoader.load(EventDumpParser.class);
		for(EventDumpParser edp: loader) {
			System.out.println(" - " + edp);
		}
	}

    private String getParserString() {
    	StringBuilder sb = new StringBuilder();
    	for(String parser: ThreadEventCodec.listSupportedFormats()) {
    		sb.append(parser).append(", ");
    	}
    	ServiceLoader<EventDumpParser> loader = ServiceLoader.load(EventDumpParser.class);
		for(EventDumpParser edp: loader) {
			if (edp.isFunctional()) {
				sb.append(edp).append(", ");
			}
		}
		if (sb.length() > 0) {
			sb.setLength(sb.length() - 2);
		}
		return sb.toString();
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
		if (new File(file).isFile() && new File(file).length() > 0) {
			host.logError("Unable to parse file '" + file + "'. Parser config: " + getParserString());
		}
		return null;
    }
    
    static class TimeFilter implements EventMorpher<Event, Event> {
        
        TimeRangeChecker checker;
        
        public TimeFilter(TimeRangeChecker checker) {
            this.checker = checker;
        }

        @Override
        public Event morph(Event event) {
        	if (event instanceof TimestampedEvent) {
        		return checker.evaluate(((TimestampedEvent)event).timestamp()) ? event : null;
        	}
        	else {
        		return null;
        	}
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
