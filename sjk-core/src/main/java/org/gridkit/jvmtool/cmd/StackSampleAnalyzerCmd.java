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

import static org.gridkit.jvmtool.stacktrace.analytics.ThreadDumpAggregatorFactory.COMMON;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.Thread.State;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.CategorizerParser;
import org.gridkit.jvmtool.StackHisto;
import org.gridkit.jvmtool.ThreadDumpSource;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.analytics.CachingFilterFactory;
import org.gridkit.jvmtool.stacktrace.analytics.ParserException;
import org.gridkit.jvmtool.stacktrace.analytics.SimpleCategorizer;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadDumpAggregatorFactory;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotCategorizer;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSplitAggregator;
import org.gridkit.jvmtool.stacktrace.analytics.TraceFilterPredicateParser;
import org.gridkit.jvmtool.stacktrace.analytics.flame.FlameGraphGenerator;
import org.gridkit.jvmtool.stacktrace.analytics.flame.RainbowColorPicker;
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
		private final CommandLauncher host;

		@ParametersDelegate
		private final ThreadDumpSource dumpSource;
		
		@Parameter(names={"-cf", "--categorizer-file"}, required = false, description="Path to file with stack trace categorization definition")
		private String categorizerFile = null;

        @Parameter(names={"-nc", "--named-class"}, required = false, variableArity = true, description="May be used with some commands to define name stack trace classes\nUse <name>=<filter expression> notation")
        private List<String> namedClasses = new ArrayList<String>();
        
        @Parameter(names={"-tz", "--time-zone"}, required = false, description="Time zone used for timestamps and time ranges")
        private String timeZone = "UTC";

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
        private SsaCmd threadInfo = new ThreadInfoCmd();
        
        @ParametersDelegate
        private SsaCmd help = new HelpCmd();

		ThreadSnapshotCategorizer categorizer;


		public SSA(CommandLauncher host) {
			this.host = host;
			this.dumpSource = new ThreadDumpSource(host);
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
				dumpSource.setTimeZone(timeZone());
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
		
		TimeZone timeZone() {
		    return TimeZone.getTimeZone(timeZone);
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
		
		ThreadSnapshotFilter parseFilter(String filter) {
		    CachingFilterFactory factory = new CachingFilterFactory();
		    ThreadSnapshotFilter tf = TraceFilterPredicateParser.parseFilter(filter, factory);
		    return tf;
		}
		
		EventReader<ThreadSnapshotEvent> getFilteredReader() {
		    return dumpSource.getFilteredReader();
		}

		EventReader<ThreadSnapshotEvent> getUnclassifiedReader() {
		    return dumpSource.getUnclassifiedReader();
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
				    
			        EventReader<ThreadSnapshotEvent> reader = getFilteredReader();
			        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			        fmt.setTimeZone(timeZone());
			        StringBuilder threadHeader = new StringBuilder();
			        StringBuilder stackFrameBuffer = new StringBuilder();
			        for(ThreadSnapshotEvent e: reader) {
			            String timestamp = fmt.format(e.timestamp());
			            threadHeader
			                .append("Thread [")
			                .append(e.threadId())
			                .append("] ");
			            if (e.threadState() != null) {
			                threadHeader.append(e.threadState()).append(' ');
			            }
			            threadHeader.append("at ").append(timestamp);
			            if (e.threadName() != null) {
			                threadHeader.append(" - ").append(e.threadName());
			            }
			            System.out.println(threadHeader);
			            StackFrameList trace = e.stackTrace();
			            for(StackFrame frame: trace) {
			            	frame.toString(stackFrameBuffer);
			                System.out.println(stackFrameBuffer);
			                stackFrameBuffer.setLength(0);
			            }
			            System.out.println();
			            threadHeader.setLength(0);
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

            @Parameter(names={"--by-term"}, description="Sort frame histogram by terminal count")
            boolean sortByTerm = false;

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
                    
                    EventReader<ThreadSnapshotEvent> reader = getFilteredReader();
                    int n = 0;
                    for(ThreadSnapshotEvent e: reader) {
                        StackFrameList trace = e.stackTrace();
                        histo.feed(trace);
                        ++n;
                    }
                    
                    if (sortByTerm) {
			histo.setHistoOrder(StackHisto.BY_TERMINAL);
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

                    FlameGraphGenerator fg = new FlameGraphGenerator();
                    if (rainbow != null && rainbow.size() > 0) {
                        ThreadSnapshotFilter[] filters = new ThreadSnapshotFilter[rainbow.size()];
                        CachingFilterFactory factory = new CachingFilterFactory();
                        for (int i = 0; i != rainbow.size(); ++i) {
                            filters[i] = TraceFilterPredicateParser.parseFilter(rainbow.get(i), factory);
                        }
                        fg.setColorPicker(new RainbowColorPicker(filters));
                    }
                    
                    EventReader<ThreadSnapshotEvent> reader = getFilteredReader();
                    int n = 0;
                    for(ThreadSnapshotEvent e: reader) {
                        StackFrameList trace = e.stackTrace();
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

                    EventReader<ThreadSnapshotEvent> reader = getUnclassifiedReader();
                    for(ThreadSnapshotEvent e: reader) {
                        String cl = cat.categorize(e);
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
                        System.out.println(TextTable.formatCsv(tt));
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

        class ThreadInfoCmd extends SsaCmd {
            
            @Parameter(names={"--thread-info"}, description="Per thread info summary")
            boolean run;

            @Parameter(names={"-si", "--summary-info"}, variableArity = true, description="List of summaries")
            List<String> summaryInfo;

            @Override
            public boolean isSelected() {
                return run;
            }
            
            List<String> summaryNames = new ArrayList<String>();
            List<ThreadDumpAggregatorFactory> summaries = new ArrayList<ThreadDumpAggregatorFactory>();
            List<SummaryFormater> summaryFormaters = new ArrayList<SummaryFormater>();

            void add(String name, ThreadDumpAggregatorFactory summary) {
                add(name, summary, new DefaultFormater());
            }

            void add(String name, ThreadDumpAggregatorFactory summary, SummaryFormater formater) {
                summaryNames.add(name + " ");
                summaries.add(summary);
                summaryFormaters.add(formater);
            }
            
            @Override
            public void run() {
                try {
                    
                    if (summaryInfo == null || summaryInfo.isEmpty()) {
                        add("Name", COMMON.name());
                        add("Count", COMMON.count(), new RightFormater());
                        add("On CPU", COMMON.cpu(), new PercentFormater());
                        add("Alloc ", COMMON.alloc(), new MemRateFormater());
                        add("RUNNABLE", COMMON.threadState(State.RUNNABLE), new PercentFormater());
                        add("Native", COMMON.inNative(), new PercentFormater());
                    }
                    else {
                        for(String si: summaryInfo) {
                            addSummary(si);
                        }
                    }
                    
                    ThreadSplitAggregator threadAgg = new ThreadSplitAggregator(summaries.toArray(new ThreadDumpAggregatorFactory[0]));
                    EventReader<ThreadSnapshotEvent> reader = getFilteredReader();
                    for(ThreadSnapshotEvent e: reader) {
                        threadAgg.feed(e);
                    }
                    
                    TextTable tt = new TextTable();
                    tt.addRow(summaryNames);
                    int n = 0;
                    for(Object[] row: threadAgg.report()) {
                        ++n;
                        String[] frow = new String[summaries.size()];
                        for(int i = 0; i != summaries.size(); ++i) {
                            SummaryFormater sf = summaryFormaters.get(i);
                            frow[i] = sf.toString(row[i + 2]) + " ";
                        }
                        tt.addRow(frow);
                    }
                    
                    if (n > 0) {
                        if (csvOutput) {
                            System.out.println(TextTable.formatCsv(tt));
                        }
                        else {
                            System.out.println(tt.formatTextTableUnbordered(80));
                        }
                    }
                    else {
                        System.out.println("No data");
                    }
                    
                } catch (Exception e) {
                    host.fail(e.toString(), e);
                }
            }
            
            private void addSummary(String si) {
                si = si.trim();
                if ("NAME".equals(si)) {
                    add("Name", COMMON.name());
                }
                else if (si.startsWith("NAME") && si.indexOf('=') < 0) {
                    int n = Integer.valueOf(si.substring(4));
                    add("Name", COMMON.name(n));
                }
                else if ("COUNT".equals(si)) {
                    add("Count", COMMON.count(), new RightFormater());
                }
                else if ("TSMIN".equals(si)) {
                    add("First time", COMMON.minTimestamp(), new DateFormater(timeZone()));
                }
                else if ("TSMAX".equals(si)) {
                    add("Last time", COMMON.maxTimestamp(), new DateFormater(timeZone()));
                }
                else if ("CPU".equals(si)) {
                    add("On CPU", COMMON.cpu(), new PercentFormater());
                }
                else if ("SYS".equals(si)) {
                	add("System", COMMON.sysCpu(), new PercentFormater());
                }
                else if ("ALLOC".equals(si)) {
                    add("Alloc ", COMMON.alloc(), new MemRateFormater());
                }
                else if (si.startsWith("S:")) {
                    State st = State.valueOf(si.substring(2));
                    add(st.toString(), COMMON.threadState(st), new PercentFormater());
                }
                else if ("NATIVE".equals(si)) {
                    add("Native", COMMON.inNative(), new PercentFormater());
                }
                else if (Pattern.matches(".*=.*", si)) {
                    String[] p = si.split("[=]");
                    if (p.length != 2) {
                        badSummary(si);
                    }
                    ThreadSnapshotFilter ts = parseFilter(p[1]);
                    add(p[0], COMMON.threadFilter(ts), new PercentFormater());
                }
                else if ("FREQ".equals(si)) {
                    add("Freq.", COMMON.frequency(), new DecimalFormater(1));
                }
                else if ("FREQ_HM".equals(si)) {
                    add("Freq. (1/HM)", COMMON.frequencyHM(), new DecimalFormater(1));
                }
                else if ("GAP_CHM".equals(si)) {
                    add("Gap CHM", COMMON.periodCHM(), new DecimalFormater(3));
                }
                else { 
                    badSummary(si);
                }
            }

            private void badSummary(String si) {
                host.fail("Unknown summary '" + si + "'",
                        "Allowed summaries are",
                        "  NAME",
                        "  NAME<len>",
                        "  COUNT",
                        "  TSMIN",
                        "  TSMAX",
                        "  CPU",
                        "  SYS",
                        "  ALLOC",
                        "  NATIVE",
                        "  FREQ",
                        "  FREQ_HM",
                        "  GAP_CHM",
                        "  S:[RUNNABLE|BLOCKED|WAITING|TIMED_WAITING]",
                        "  <name>=<filter expression>"
                        );
                
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
		
	interface SummaryFormater {
	    
	    public String toString(Object summary);
	}
	
	static class DefaultFormater implements SummaryFormater {

        @Override
        public String toString(Object summary) {
            return String.valueOf(summary);
        }
	}

	static class RightFormater implements SummaryFormater {
	    
	    @Override
	    public String toString(Object summary) {
	        return "\t" + String.valueOf(summary);
	    }
	}

	static class DecimalFormater implements SummaryFormater {
	    
	    int n;
	    
	    public DecimalFormater(int n) {
            this.n = n;
        }
	    
	    @Override
	    public String toString(Object summary) {
	        if (summary instanceof Long) {
	            return "\t" + summary;
	        }
	        else if (summary instanceof Number) {
	            return "\t" + String.format("%." + n +"f", ((Number) summary).doubleValue());
	        }
	        else {
	            return "";
	        }
	    }
	}

	static class PercentFormater implements SummaryFormater {
	    
	    @Override
	    public String toString(Object summary) {
            if (summary instanceof Number) {
                double d = ((Number) summary).doubleValue();
                if (!Double.isNaN(d)) {
                    return String.format("\t%.1f%%", 100 * d);
                }
            }
            return "";
	    }
	}
	
	static class MemRateFormater implements SummaryFormater {

        @Override
        public String toString(Object summary) {
            if (summary instanceof Number) {
                double d = ((Number) summary).doubleValue();
                if (!Double.isNaN(d)) {
                    return Formats.toMemorySize((long)d) + "/s";
                }
            }
            return "";
        }
	}

	static class DateFormater implements SummaryFormater {
	    
	    SimpleDateFormat fmt;
	    
	    public DateFormater(TimeZone tz) {
            fmt = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");
            fmt.setTimeZone(tz);
        }
	    
	    @Override
	    public String toString(Object summary) {
	        if (summary instanceof Long) {
	            return fmt.format(summary);
	        }
	        else {
	            return "";
	        }
	    }
	}
}
