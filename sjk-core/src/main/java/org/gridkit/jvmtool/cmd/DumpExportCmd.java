package org.gridkit.jvmtool.cmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.gridkit.jvmtool.AbstractEventDumpSource;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.ShieldedEventReader;
import org.gridkit.jvmtool.event.TagCollection;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.util.formating.AsciiTableFormatter;
import org.gridkit.util.formating.CsvTableFormatter;
import org.gridkit.util.formating.TableFormatter;
import org.gridkit.util.formating.TabularDataSink.Cursor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class DumpExportCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "dexp";
	}

	@Override
	public Runnable newCommand(CommandLauncher host) {
		return new DumpExport(host);
	}

	@Parameters(commandDescription = "[Dump Export] Extract metrics form compressed dump into tabular format")
	public static class DumpExport implements Runnable {

		@ParametersDelegate
		private CommandLauncher host;
				
		@ParametersDelegate
		private DumpInput input;
		
        @Parameter(names = { "--tags" }, description = "Output statistics for tags")
        private boolean printTags;
		
	    @Parameter(names = {"-tz", "--timezone"}, required = false, description = "Set time zone to be used for date formating")
	    private String timezone;

	    @Parameter(names = {"-o", "--outfile"}, required = false, description = "Out data into a file instead of std out")
	    private String outfile;
	    
        @Parameter(names = {"-cl", "--columns"}, required = false, variableArity = true, description = "List of columns (tags) to be exported")
        private List<String> columns = new ArrayList<String>();

        @Parameter(names = {"-csv"}, required = false, description = "Format output as CSV")
        private boolean csv = false; 
        
        @Parameter(names = {"--explain"}, required = false, description = "Include additional information into std out")
        private boolean explain = false;

        @Parameter(names = {"--export-all"}, required = false, description = "Export all columns")
        private boolean exportAll = false;

        private TimeZone tz = TimeZone.getTimeZone("UTC");
        
		public DumpExport(CommandLauncher host) {
			this.host = host;
			this.input = new DumpInput(host);
		}
		
		@Override
		public void run() {
			
			try {

				if (timezone != null) {
					tz = TimeZone.getTimeZone(timezone);
				}
				
				input.setTimeZone(tz);
				
				if (explain) {
					System.out.println("Input files");
					for(String f: input.sourceFiles()) {
						System.out.println("  " + f);
					}
					System.out.println();
				}
			    
			    Writer writer;
			    if (outfile != null) {
			    	File f = new File(outfile);
			    	if (f.exists()) {
			    		host.fail("File already exists [" + outfile + "]");
			    	}
			    	if (f.getParentFile() != null) {
			    		f.getParentFile().mkdirs();
			    	}
			    	FileOutputStream fos = new FileOutputStream(f);
			    	writer = new OutputStreamWriter(fos, Charset.forName("UTF8"));
			    }
			    else {
			    	writer = new OutputStreamWriter(System.out, Charset.forName("UTF8"));
			    }			    

			    if (printTags) {
			    	printTags(openReader(), writer);
			    }
			    else {
			    	if (exportAll) {
			    		columns = enumColumns(openReader());
			    	}
			    	if (columns.isEmpty()) {
			    		host.fail("Export column list is empty");
			    	}
			    	export(openReader(), writer, columns);
			    }
			    writer.close();
			    
			} catch (Exception e) {
				host.fail("Unexpected error: " + e.toString(), e);
			}			
		}

		private EventReader<CommonEvent> openReader() {
			final EventReader<CommonEvent> reader = new ShieldedEventReader<CommonEvent>(input.getFilteredRawReader(), CommonEvent.class);
				
			return reader;
		}		
		
		private void printTags(EventReader<CommonEvent> openReader, Writer writer) {
			long minTs = Long.MAX_VALUE;
			long maxTs = Long.MIN_VALUE;
			long count = 0;
			Histo histo = new Histo();
			
			for(CommonEvent e: openReader) {
				count++;
				minTs = Math.min(e.timestamp(), minTs);
				maxTs = Math.max(e.timestamp(), maxTs);
				histo.count(e);
			}			
			
			List<TagStat> tags = new ArrayList<TagStat>(histo.histo.values());
			Collections.sort(tags, new TagStatComparator());
			
			PrintWriter pw = new PrintWriter(writer);
			
			if (count == 0) {
				pw.println("No events");
			}
			else {
				pw.println("Event count: " + count);			
				pw.println("Time range: " + fdate(minTs) + " - " + fdate(maxTs));
				for(TagStat ts: tags) {
					pw.print(" @" + ts.key + " - " + ts.count);
					if (ts.values != null) {
						String vals = ts.values.toString();
						if (vals.length() > 40) {
							vals = vals.substring(0, 36) + " ...";
						}
						pw.print("  " + vals);
					}
					pw.println();
				}
			}			
			pw.flush();
		}
		
		private String fdate(long ts) {
			if (ts == Long.MAX_VALUE || ts == Long.MIN_VALUE) {
				return "";
			}
			else {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HH:mm");
				sdf.setTimeZone(tz);
				return sdf.format(ts);
			}
		}

		private void export(EventReader<CommonEvent> openReader, Writer writer, List<String> cols) throws IOException {
			TableFormatter table = csv ? new CsvTableFormatter(tz) : new AsciiTableFormatter(tz);
			boolean exportT = false;
			List<String> fcols = new ArrayList<String>();
			for (String col: cols) {
				String cn = col;
				String fmt = null;
				if (col.indexOf(':') >= 0) {
					cn = col.substring(0, col.indexOf(':'));
					fmt = col.substring(cn.length() + 1);
				}
				if (fmt == null) {
					table.addCol(cn);
				}
				else {
					table.addCol(cn, cn, fmt);					
				}
				if (cn.equals("T")) {
					exportT = true;
				}
				else {
					fcols.add(cn);
				}
			}
			Cursor c = table.newCursor();
			for(CommonEvent ce: openReader) {
				boolean hasValue = false;
				for(String col: fcols) {
					String v = ce.tags().firstTagFor(col);
					if (v != null) {
						c.setCell(col, v);
						hasValue = true;
					}
					long lv = ce.counters().getValue(col);
					if (lv >= 0) {
						c.setCell(col, String.valueOf(lv));
						hasValue = true;
					}
				}
				if (hasValue) {
					if (exportT) {
						c.setCell("T", ce.timestamp());
					}
					c.submit();
				}
			}			
			table.format(writer);
		}

		private List<String> enumColumns(EventReader<CommonEvent> reader) {
			Set<String> cols = new LinkedHashSet<String>();
			for(CommonEvent ce: reader) {
				for(String key: ce.tags()) {
					cols.add(key);
				}
				for(String key: ce.counters()) {
					cols.add(key);
				}
			}
			return new ArrayList<String>(cols);
		}

	}
	
	static class DumpInput extends AbstractEventDumpSource {
		
		@Parameter(names = {"-f", "--file"}, description = "Input files", required = true, variableArity = true)
		private List<String> inputFiles = new ArrayList<String>();

		public DumpInput(CommandLauncher host) {
			super(host);
		}

		@Override
		protected List<String> inputFiles() {

			return inputFiles;
		}
	}
	
	private static class Histo {
		
		Map<String, TagStat> histo = new HashMap<String, TagStat>();
		
		public void count(CommonEvent ce) {
			countTags(ce.tags());
			countCounter(ce.counters());
		}

		private void countTags(TagCollection tags) {
			for(String key: tags) {
				TagStat kh = histo.get(key);
				if (kh == null) {
					kh = new TagStat(key);
					histo.put(key, kh);
				}
				kh.count += 1;
				if (kh.values != null) {
					for(String val: tags.tagsFor(key)) {
						kh.addValue(val);
					}
				}
			}			
		}
		
		private void countCounter(CounterCollection counters) {
			for(String key: counters) {
				TagStat kh = histo.get(key);
				if (kh == null) {
					kh = new TagStat(key);
					histo.put(key, kh);
				}
				kh.count += 1;
				kh.values = null;
			}
		}
	}
	
	private static class TagStat {
		
		String key;
		long count;
		Set<String> values = new HashSet<String>();
		
		public TagStat(String key) {
			this.key = key;
		}

		public void addValue(String val) {
			if (values != null) {
				values.add(val);
			}
			if (values.size() > 20) {
				// TODO unique values sketch
				values = null;				
			}			
		}
	}
	
	static class TagStatComparator implements Comparator<TagStat> {

		@Override
		public int compare(TagStat o1, TagStat o2) {
			if (o1.count < o2.count) {
				return 1;
			}
			else if (o1.count > o2.count) {
				return -1;
			}
			else {
				return o1.key.compareTo(o2.key);
			}
		}
	}
}
