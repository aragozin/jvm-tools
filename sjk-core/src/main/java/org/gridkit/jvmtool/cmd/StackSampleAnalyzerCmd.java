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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;
import org.gridkit.jvmtool.StackHisto;
import org.gridkit.jvmtool.StackTraceCodec;
import org.gridkit.jvmtool.StackTraceCodec.StackTraceReader;
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

	@Parameters(commandDescription = "Analyzing stack trace dumps")
	public static class SSA implements Runnable {
		
		@SuppressWarnings("unused")
		@ParametersDelegate
		private SJK host;
		
		@Parameter(names={"-f", "--file"}, required = true, variableArity=true, description="Path to stack dump file")
		private List<String> files;
		
		@ParametersDelegate
		private SsaCmd print = new PrintCmd();

		@ParametersDelegate
		private SsaCmd histo = new HistoCmd();

		private List<SsaCmd> allCommands = Arrays.asList(print, histo);

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
				action.get(0).run();
			} catch (Exception e) {
				SJK.fail(e.toString());
			}
		}

		abstract class SsaCmd implements Runnable {
		    
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
				    
				    for(String file: files) {
				        StackTraceReader reader = StackTraceCodec.newReader(new FileInputStream(file));
				        while(reader.loadNext()) {
				            String timestamp = Formats.toDatestamp(reader.getTimestamp());
				            System.out.println(String.format("Thread [%d] at %s", reader.getThreadId(), timestamp));
				            StackTraceElement[] trace = reader.getTrace();
				            for(int i = trace.length; i != 0; --i) {
				                System.out.println(trace[i - 1]);
				            }
				            System.out.println();
				        }
				    }
				    
				} catch (Exception e) {
					e.printStackTrace();
					SJK.fail();
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
                    
                    for(String file: files) {
                        StackTraceReader reader = StackTraceCodec.newReader(new FileInputStream(file));
                        while(reader.loadNext()) {
                            StackTraceElement[] trace = reader.getTrace();
                            histo.feed(trace);
                        }
                    }
                    
                    System.out.println(histo.formatHisto());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    SJK.fail();
                }
            }
            
            public String toString() {
                return "--histo";
            }
        }
	}	
}
