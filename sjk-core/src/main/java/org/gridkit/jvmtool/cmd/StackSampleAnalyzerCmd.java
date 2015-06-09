/**
 * Copyright 2014 Alexey Ragozin
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
package org.gridkit.jvmtool.cmd;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.Cascade;
import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;
import org.gridkit.jvmtool.StackHisto;
import org.gridkit.jvmtool.StackTraceClassifier;
import org.gridkit.jvmtool.StackTraceClassifier.Config;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.util.formating.Formats;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class StackSampleAnalyzerCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "ssa";
	}

	@Override
	public Runnable newCommand(SJK host) {
		return new SSA(host);
	}

	@Parameters(commandDescription = "[Stack Sample Analyzer] Analyzing stack trace dumps")
	public static class SSA implements Runnable {
		
		@ParametersDelegate
		private SJK host;
		
		@Parameter(names={"-f", "--file"}, required = true, variableArity=true, description="Path to stack dump file")
		private List<String> files;
		
		@Parameter(names={"-c", "--classifier"}, required = false, description="Path to file with stack trace classification definition")
		private String classifier = null;

        @Parameter(names = { "-b", "--buckets" }, required = false, description = "Restrict analysis to specific class")
        private String bucket = null;

        @Parameter(names={"-sf", "--simple-filter"}, required = false, description="Process only traces containing this string")
        private String simpleFilter = null;

        
        private List<SsaCmd> allCommands = new ArrayList<SsaCmd>();

        @ParametersDelegate
		private SsaCmd print = new PrintCmd();

        @ParametersDelegate
		private SsaCmd histo = new HistoCmd();

        @ParametersDelegate
		private SsaCmd csummary = new ClassSummaryCmd();

		StackTraceClassifier buckets;


		public SSA(SJK host) {
			this.host = host;
		}

		@Override
		public void run() {
			try {
				List<Runnable> action = new ArrayList<Runnable>();
				for(SsaCmd cmd: allCommands) {
				    if (cmd.isSelected()) {
				        action.add(cmd);
				    }
				}
				if (action.isEmpty() || action.size() > 1) {
					SJK.failAndPrintUsage("You should choose one of " + allCommands);
				}
				if (classifier != null) {
				    Config cfg = new Config();
				    Cascade.parse(new FileReader(classifier), cfg);
				    buckets = cfg.create();
				}
				if (classifier == null  && bucket != null) {
				    SJK.failAndPrintUsage("--bucket option requires --classifer");
				}
				action.get(0).run();
			} catch (Exception e) {
				SJK.fail(e.toString(), e);
			}
		}

		StackTraceReader getFilteredReader() throws IOException {
		    if (classifier == null ) {
		        if (simpleFilter == null) {
		            return getUnclassifiedReader();
		        }
		        else {
		            final StackTraceReader unclassified = getUnclassifiedReader();
		            return new StackTraceReader.StackTraceReaderDelegate() {

                        @Override
                        protected StackTraceReader getReader() {
                            return unclassified;
                        }
                        
                        @Override
                        public boolean loadNext() throws IOException {
                            while(unclassified.loadNext()) {
                                StackTraceElement[] trace = getTrace();
                                for(StackTraceElement e: trace) {
                                    if (e.toString().startsWith(simpleFilter)) {
                                        return true;
                                    }
                                }
                            }
                            return false;
                        }
                        
                        @Override
                        public boolean isLoaded() {
                            return unclassified.isLoaded();
                        }
                        
                        @Override
                        public StackTraceElement[] getTrace() {
                            return unclassified.getTrace();
                        }
                        
                        @Override
                        public long getTimestamp() {
                            return unclassified.getTimestamp();
                        }
                        
                        @Override
                        public long getThreadId() {
                            return unclassified.getThreadId();
                        }
                    };
		        }
		    }
		    else {
		        if (simpleFilter != null) {
		            SJK.fail("Simple filter cannot be used with classification");
		        }		        
		        if (bucket != null && !buckets.getClasses().contains(bucket)) {
		            SJK.fail("Bucket [" + bucket + "] is not defined");
		        }
		        final StackTraceReader unfiltered = getUnclassifiedReader();
		        return new StackTraceReader.StackTraceReaderDelegate() {

                    @Override
                    protected StackTraceReader getReader() {
                        return unfiltered;
                    }

                    @Override
                    public boolean loadNext() throws IOException {
                        while(true) {
                            if (unfiltered.loadNext()) {
                                String cl = buckets.classify(unfiltered.getTrace());
                                if (bucket != null) {
                                    if (!bucket.equals(cl)) {
                                        continue;
                                    }
                                }
                                else if (cl == null) {
                                    continue;
                                }
                                return true;
                            }
                            else {
                                return false;
                            }
                        }
                    }

                    @Override
                    public boolean isLoaded() {
                        return unfiltered.isLoaded();
                    }

                    @Override
                    public StackTraceElement[] getTrace() {
                        return unfiltered.getTrace();
                    }

                    @Override
                    public long getTimestamp() {
                        return unfiltered.getTimestamp();
                    }

                    @Override
                    public long getThreadId() {
                        return unfiltered.getThreadId();
                    }
                };
		    }
		}

		StackTraceReader getUnclassifiedReader() throws IOException {
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

		abstract class SsaCmd implements Runnable {
		    
		    public SsaCmd() {
                allCommands.add(this);
            }

		    public abstract boolean isSelected();
		}
		
		class PrintCmd extends SsaCmd {

			@Parameter(names={"--print"}, description="Print traces from file")
			boolean run;

			@Override
            public boolean isSelected() {
                return run;
            }

            @Override
			public void run() {
				try {
				    
			        StackTraceReader reader = getFilteredReader();
			        while(reader.loadNext()) {
			            String timestamp = Formats.toDatestamp(reader.getTimestamp());
			            StringBuilder threadHeader = new StringBuilder();
			            threadHeader
			                .append("Thread [")
			                .append(reader.getThreadId())
			                .append("] ");
			            if (reader.getThreadState() != null) {
			                threadHeader.append(reader.getThreadState()).append(' ');
			            }
			            threadHeader.append("at ").append(timestamp);
			            if (reader.getThreadName() != null) {
			                threadHeader.append(" - ").append(reader.getThreadName());
			            }
			            System.out.println(threadHeader);
			            StackTraceElement[] trace = reader.getTrace();
			            for(int i = 0; i != trace.length; ++i) {
			                System.out.println(trace[i]);
			            }
			            System.out.println();
			        }
				    
				} catch (Exception e) {
					SJK.fail(e.toString(), e);
				}
			}
            
            public String toString() {
                return "--print";
            }
		}

        class HistoCmd extends SsaCmd {

            @Parameter(names={"--histo"}, description="Print frame histogram")
            boolean run;

            @Override
            public boolean isSelected() {
                return run;
            }

            @Override
            public void run() {
                try {

                    StackHisto histo = new StackHisto();
                    
                    StackTraceReader reader = getFilteredReader();
                    int n = 0;
                    while(reader.loadNext()) {
                        StackTraceElement[] trace = reader.getTrace();
                        histo.feed(trace);
                        ++n;
                    }
                    
                    if (n > 0) {
                    System.out.println(histo.formatHisto());
                    }
                    else {
                        System.out.println("No data");
                    }
                    
                } catch (Exception e) {
                    SJK.fail(e.toString(), e);
                }
            }
            
            public String toString() {
                return "--histo";
            }
        }

        class ClassSummaryCmd extends SsaCmd {

            @Parameter(names={"--summary"}, description="Print summary for provided classification")
            boolean run;

            @Override
            public boolean isSelected() {
                return run;
            }

            @Override
            public void run() {
                try {

                    if (classifier == null) {
                        SJK.failAndPrintUsage("Classification is required");
                    }
                    if (bucket != null) {
                        SJK.failAndPrintUsage("--summary cannot be used with --bucket option");
                    }
                    List<String> bucketNames = new ArrayList<String>(buckets.getClasses());
                    long[] counters = new long[bucketNames.size()];
                    long total = 0;

                    StackTraceReader reader = getUnclassifiedReader();
                    while(reader.loadNext()) {
                        StackTraceElement[] trace = reader.getTrace();
                        String cl = buckets.classify(trace);
                        if (cl != null) {
                            ++total;
                            ++counters[bucketNames.indexOf(cl)];
                        }
                    }

                    System.out.println(String.format("%-40s\t%d\t%.2f%%", "Total samples", total, 100f));
                    for(int i = 0; i != counters.length; ++i) {
                        System.out.println(String.format("%-40s\t%d\t%.2f%%", bucketNames.get(i), counters[i], (100f * counters[i]) / total));
                    }

                } catch (Exception e) {
                    SJK.fail(e.toString(), e);
                }
            }

            public String toString() {
                return "--summary";
            }
        }
	}	
}
