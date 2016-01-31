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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.jvmtool.CategorizerParser;
import org.gridkit.jvmtool.StackHisto;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.ReaderProxy;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.jvmtool.stacktrace.analytics.CachingFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.FilteredStackTraceReader;
import org.gridkit.jvmtool.stacktrace.analytics.FlameGraph;
import org.gridkit.jvmtool.stacktrace.analytics.ParserException;
import org.gridkit.jvmtool.stacktrace.analytics.PositionalStackMatcher;
import org.gridkit.jvmtool.stacktrace.analytics.RainbowColorPicker;
import org.gridkit.jvmtool.stacktrace.analytics.SimpleCategorizer;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotCategorizer;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.jvmtool.stacktrace.analytics.TraceFilterPredicateParser;
import org.gridkit.util.formating.Formats;
import org.gridkit.util.formating.TextTable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class StackSampleAnalyzerCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "ssa";
	}

	@Override
	public Runnable newCommand(CommandLauncher host) {
		return new SSA(host);
	}

	@Parameters(commandDescription = "[Stack Sample Analyzer] Analyzing stack trace dumps")
	public static class SSA implements Runnable {
		
		@ParametersDelegate
		private CommandLauncher host;
		
		@Parameter(names={"-f", "--file"}, required = false, variableArity=true, description="Path to stack dump file")
		private List<String> files;
		
		@Parameter(names={"-cf", "--categorizer-file"}, required = false, description="Path to file with stack trace categorization definition")
		private String categorizerFile = null;

        @Parameter(names={"-nc", "--named-class"}, required = false, variableArity = true, description="May be used with some commands to define name stack trace classes\nUse <name>=<filter expression> notation")
        private List<String> namedClasses = new ArrayList<String>();
        
        @Parameter(names={"-tf", "--trace-filter"}, required = false, description="Apply filter to traces before processing. Use --ssa-help for more details about filter notation")
        private String traceFilter = null;

        @Parameter(names={"-tt", "--trace-trim"}, required = false, description="Positional filter trim frames to process. Use --ssa-help for more details about filter notation")
        private String traceTrim = null;

        @Parameter(names={"-co", "--csv-output"}, required = false, description="Output data in CSV format")
        private boolean csvOutput = false;
        
        private List<SsaCmd> allCommands = new ArrayList<SsaCmd>();

        @ParametersDelegate
		private SsaCmd print = new PrintCmd();

        @ParametersDelegate
		private SsaCmd histo = new HistoCmd();

        @ParametersDelegate
		private SsaCmd flame = new FlameCmd();

        @ParametersDelegate
        private SsaCmd csummary = new CategorizeCmd();

        @ParametersDelegate
        private SsaCmd help = new HelpCmd();

		ThreadSnapshotCategorizer categorizer;


		public SSA(CommandLauncher host) {
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
					host.failAndPrintUsage("You should choose one of " + allCommands);
				}
				if (categorizerFile != null) {
				    if (!namedClasses.isEmpty()) {
				        host.failAndPrintUsage("You eigther should specify categorizer (-cf) or named classed (-nc)");
				    }
				    try {
                        FileReader csource = new FileReader(categorizerFile);
                        SimpleCategorizer sc = new SimpleCategorizer();
                        CachingFilterFactory cff = new CachingFilterFactory();
                        CategorizerParser.loadCategories(csource, sc, false, cff);
                        categorizer = sc;
                    } catch (ParserException e) {
                        throw host.fail("Failed to parse filter expression at [" + e.getOffset() + "] : " + e.getMessage(), e.getParseText());
                    }
				}
				action.get(0).run();
			} catch (Exception e) {
			    host.fail(e.toString(), e);
			}
		}

		Map<String, ThreadSnapshotFilter> getNamedClasses() {
		    if (namedClasses.isEmpty()) {
		        return new HashMap<String, ThreadSnapshotFilter>();
		    }
		    else {
		        CachingFilterFactory factory = new CachingFilterFactory();
		        Map<String, ThreadSnapshotFilter> classes = new LinkedHashMap<String, ThreadSnapshotFilter>();
		        for(String nc: namedClasses) {
		            int n = nc.indexOf('=');
		            if (n < 0) {
		                throw host.fail("Cannot parse named class", "[" + nc + "]", "Required format NAME=FILTER_EXPRESSION");
		            }
		            String name = nc.substring(0, n);
		            String filter = nc.substring(n + 1);
		            if (classes.containsKey(name)) {
		                throw host.fail("Duplicated class name [" + name + "]");		                
		            }
		            try {
                        ThreadSnapshotFilter tf = TraceFilterPredicateParser.parseFilter(filter, factory);
                        classes.put(name, tf);
                    } catch (ParserException e) {
                        throw host.fail("Cannot parse named class", "[" + nc + "]", e.getMessage());
                    }
		        }
		        return classes;
		    }
		}
		
		StackTraceReader getFilteredReader() throws IOException {
	        if (traceFilter == null && traceTrim == null) {
	            return getUnclassifiedReader();
	        }
	        else {
	            StackTraceReader reader = getUnclassifiedReader();
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

		StackTraceReader getUnclassifiedReader() throws IOException {
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
					host.fail(e.toString(), e);
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
                    for(Map.Entry<String, ThreadSnapshotFilter> entry: getNamedClasses().entrySet()) {
                        histo.addCondition(entry.getKey(), entry.getValue());
                    }
                    
                    StackTraceReader reader = getFilteredReader();
                    int n = 0;
                    while(reader.loadNext()) {
                        StackFrameList trace = reader.getStackTrace();
                        histo.feed(trace);
                        ++n;
                    }
                    
                    if (n > 0) {
                        if (csvOutput) {
                            System.out.println(histo.formatHistoToCSV());
                        }
                        else {
                            System.out.println(histo.formatHisto());
                        }
                    }
                    else {
                        System.out.println("No data");
                    }
                    
                } catch (Exception e) {
                    host.fail(e.toString(), e);
                }
            }
            
            public String toString() {
                return "--histo";
            }
        }

        class FlameCmd extends SsaCmd {

            @Parameter(names={"--flame"}, description="Export flame graph to SVG format")
            boolean run;
            
            @Parameter(names={"--title"}, description="Flame graph title")
            String title = "Flame Graph";
            
            @Parameter(names={"--width"}, description="Flame graph width in pixels")
            int width = 1200;
            
            @Parameter(names={"-rc", "--rainbow"}, variableArity = true, description="List of filters for rainbow coloring")
            List<String> rainbow;

            @Override
            public boolean isSelected() {
                return run;
            }

            @Override
            public void run() {
                try {

                    FlameGraph fg = new FlameGraph();
                    if (rainbow != null && rainbow.size() > 0) {
                        ThreadSnapshotFilter[] filters = new ThreadSnapshotFilter[rainbow.size()];
                        CachingFilterFactory factory = new CachingFilterFactory();
                        for (int i = 0; i != rainbow.size(); ++i) {
                            filters[i] = TraceFilterPredicateParser.parseFilter(rainbow.get(i), factory);
                        }
                        fg.setColorPicker(new RainbowColorPicker(filters));
                    }
                    
                    StackTraceReader reader = getFilteredReader();
                    int n = 0;
                    while(reader.loadNext()) {
                        StackFrameList trace = reader.getStackTrace();
                        fg.feed(trace);
                        ++n;
                    }
                    
                    if (n > 0) {
                        Writer w = new OutputStreamWriter(System.out);
                        fg.renderSVG(title, width, w);
                        w.flush();
                    }
                    else {
                        System.out.println("No data");
                    }
                    
                } catch (Exception e) {
                    host.fail(e.toString(), e);
                }
            }
            
            public String toString() {
                return "--histo";
            }
        }
        
        class CategorizeCmd extends SsaCmd {

            @Parameter(names={"--categorize"}, description="Print summary for provided categorization")
            boolean run;

            @Override
            public boolean isSelected() {
                return run;
            }

            @Override
            public void run() {
                try {

                    ThreadSnapshotCategorizer cat = categorizer;
                    if (categorizer == null) {
                        if (!namedClasses.isEmpty()) {
                            SimpleCategorizer sc = new SimpleCategorizer();
                            Map<String, ThreadSnapshotFilter> nf = getNamedClasses();
                            for(String fn: nf.keySet()) {
                                sc.addCategory(fn, nf.get(fn));
                            }
                            cat = sc;                            
                        }
                    }
                    
                    if (cat == null) {
                        throw host.fail("Neigther -cf nor -nc. Eigther of them is required.");
                    }
                    
                    List<String> bucketNames = new ArrayList<String>(cat.getCategories());
                    long[] counters = new long[bucketNames.size()];
                    long total = 0;

                    StackTraceReader reader = getUnclassifiedReader();
                    ReaderProxy proxy = new ReaderProxy(reader);
                    while(reader.loadNext()) {
                        String cl = cat.categorize(proxy);
                        if (cl != null) {
                            ++total;
                            ++counters[bucketNames.indexOf(cl)];
                        }
                    }

                    TextTable tt = new TextTable();
                    String tab = csvOutput ? "" : "\t ";

                    tt.addRow("Total samples", tab + total,  tab + "100.00%");
                    
                    
                    for(int i = 0; i != counters.length; ++i) {
                        tt.addRow(bucketNames.get(i), tab + counters[i],  tab + (counters[i] == 0 ? "0.00%" : String.format("%.2f%%", (100f * counters[i]) / total)));
                    }
                    
                    if (csvOutput) {
                        System.out.println(tt.formatToCSV());
                    }
                    else {
                        System.out.println(tt.formatTextTableUnbordered(Integer.MAX_VALUE));
                    }

                } catch (Exception e) {
                    host.fail(e.toString(), e);
                }
            }

            public String toString() {
                return "--categorize";
            }
        }
        
        public class HelpCmd extends SsaCmd {

            @Parameter(names={"--ssa-help"}, description="Additional information about SSA")
            boolean run;

            @Override
            public boolean isSelected() {
                return run;
            }

            @Override
            public void run() {
                try {
                    InputStream is  = getClass().getResourceAsStream("ssa-help.md");
                    if (is == null) {
                        System.out.println("Failed to load help");
                        return;
                    }
                    System.out.println();
                    byte[] buf = new byte[4 << 10];
                    while(true) {
                        int n = is.read(buf);
                        if (n < 0) {
                            break;
                        }
                        else {
                            System.out.write(buf, 0, n);
                        }
                    }
                    System.out.println();
                } catch (IOException e) {
                    System.out.println("Failed to load help");
                }
            }

            public String toString() {
                return "--ssa-help";
            }
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
