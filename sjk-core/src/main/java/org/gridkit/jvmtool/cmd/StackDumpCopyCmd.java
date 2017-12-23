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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.gridkit.jvmtool.AbstractThreadDumpSource;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.stacktrace.LegacyStackReader;
import org.gridkit.jvmtool.stacktrace.ReaderProxy;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameArray;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.jvmtool.stacktrace.StackTraceWriter;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Stack capture command.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackDumpCopyCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "stcpy";
	}

	@Override
	public Runnable newCommand(CommandLauncher host) {
		return new StCpy(host);
	}

	@Parameters(commandDescription = "[Stack Copy] Stack dump copy/filtering utility")
	public static class StCpy implements Runnable {

		@ParametersDelegate
		private CommandLauncher host;
				
		@ParametersDelegate
		private DumpInput input;
		
		@Parameter(names = {"-e", "--empty"}, description = "Retain threads without stack trace in dump (ignored by default)")
		private boolean retainEmptyTraces = false;

        @Parameter(names = { "--mask" }, variableArity = true, description = "One or more masking rules. E.g. com.mycompany:com.somecomplany")
        private List<String> maskingRules = new ArrayList<String>();
		
	    @Parameter(names = {"-o", "--output"}, required = true, description = "Name of file to write thread dump")
	    private String outputFile;

        @Parameter(names = {"-ss", "--subsample"}, required = false, description = "If below 1.0 some frames will be randomly throwen away. E.g. 0.1 - every 10th will be retained")
        private double subsample = 1d;

        @Parameter(names={"-tz", "--time-zone"}, required = false, description="Time zone used for timestamps and time ranges")
        private String timeZone = "UTC";
	    
	    private int traceCounter;
        private StackTraceWriter writer;
        private List<MaskRule> masking = new ArrayList<MaskRule>();
        private Random rnd = new Random(1);
		
		public StCpy(CommandLauncher host) {
			this.host = host;
			this.input = new DumpInput(host);
		}
		
		@Override
		public void run() {
			
			try {
				
				TimeZone tz = TimeZone.getTimeZone(timeZone);
				input.setTimeZone(tz);				
			    
			    for(String rule: maskingRules) {
			        String[] parts = rule.split("[:]");
			        if (parts.length != 2) {
			            host.fail("Bad masking pattern [" + rule + "] should be int [match:replace] format");
			        }
			        masking.add(new MaskRule(parts[0], parts[1]));
			    }
			    
			    System.out.println("Input files");
			    
			    for(String f: input.sourceFiles()) {
	                System.out.println("  " + f);
			    }
			    System.out.println();
			    
			    openWriter();
			    
			    final StackTraceReader rawReader = new LegacyStackReader(input.getFilteredReader());
			    
			    StackTraceReader reader = new StackTraceReader.StackTraceReaderDelegate() {

                    @Override
                    protected StackTraceReader getReader() {
                        return rawReader;
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
			    
			    if (!reader.isLoaded()) {
			        reader.loadNext();
			    }
			    
			    ReaderProxy proxy = new ReaderProxy(reader) {

                    @Override
                    public StackFrameList stackTrace() {
                        return mask(reader.getStackTrace());
                    }

			    };
			    
			    StackWriterProxy writerProxy = new StackWriterProxy();
			    
			    while(reader.isLoaded()) {
			        writerProxy.write(proxy);
			        reader.loadNext();
			    }	
			    
			    System.out.println(traceCounter + " traces written");
			    writer.close();
				
			} catch (Exception e) {
				host.fail("Unexpected error: " + e.toString(), e);
			}			
		}

        private class StackWriterProxy implements StackTraceWriter {

            public StackWriterProxy() {
            }

            @Override
            public void write(ThreadSnapshot snap) throws IOException {
                if ((snap.stackTrace() == null || snap.stackTrace().isEmpty()) && !retainEmptyTraces) {
                    return;
                }
                
                if (rnd.nextDouble() > subsample) {
                    // ignore sample
                    return;
                }
                                
                ++traceCounter;
                writer.write(snap);
            }

            @Override
            public void close() {
                writer.close();
            }
        }

        private void openWriter() throws FileNotFoundException, IOException {
            File file = new File(outputFile);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            writer = StackTraceCodec.newWriter(new FileOutputStream(file));
            System.out.println("Writing to " + file.getAbsolutePath());
        }

        private StackFrameList mask(StackFrameList stackTrace) {
            if (maskingRules.isEmpty()) {
                return stackTrace;
            }
            else {
                StackFrame[] frames = stackTrace.toArray();
                for(int i = 0; i != frames.length; ++i) {
                    frames[i] = mask(frames[i]);
                }
                return new StackFrameArray(frames);
            }
        }

        private StackFrame mask(StackFrame stackFrame) {
            for(MaskRule rule: masking) {
                if (stackFrame.getClassName().startsWith(rule.match)) {
                    String cn = stackFrame.getClassName();
                    String nn = rule.replace + cn.substring(rule.match.length());
                    StackFrame ff = new StackFrame("", nn, stackFrame.getMethodName(), stackFrame.getSourceFile(), stackFrame.getLineNumber());
                    return ff;
                }
            }
            return stackFrame;
        }
	}
	
	static class MaskRule {
        String match;
	    String replace;
	    public MaskRule(String match, String replace) {
	        this.match = match;
	        this.replace = replace;
	    }
	}
	
	static class DumpInput extends AbstractThreadDumpSource {
		
		@Parameter(names = {"-i", "--input"}, description = "Input files", required = true, variableArity = true)
		private List<String> inputFiles = new ArrayList<String>();

		public DumpInput(CommandLauncher host) {
			super(host);
		}

		@Override
		protected List<String> inputFiles() {

			return inputFiles;
		}
	}
}
