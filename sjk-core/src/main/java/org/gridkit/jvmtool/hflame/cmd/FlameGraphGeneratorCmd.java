/**
 * Copyright 2018 Alexey Ragozin
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
package org.gridkit.jvmtool.hflame.cmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.gridkit.jvmtool.AbstractThreadDumpSource;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.hflame.FlameTemplateProcessor;
import org.gridkit.jvmtool.hflame.JsonFlameDataSet;
import org.gridkit.jvmtool.hflame.XmlUtil;
import org.w3c.dom.Document;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Stack capture command.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class FlameGraphGeneratorCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "flame";
	}

	@Override
	public Runnable newCommand(CommandLauncher host) {
		return new FlameGen(host);
	}

	@Parameters(commandDescription = "Generates flame graph from stack traces")
	public static class FlameGen implements Runnable {

		@ParametersDelegate
		private CommandLauncher host;
				
		@ParametersDelegate
		private DumpInput input;
		
	    @Parameter(names = {"-o", "--output"}, required = true, description = "Name of generated report file")
	    private String outputFile;

        @Parameter(names={"-tz", "--timezone", "--time-zone"}, required = false, description="Time zone used for timestamps and time ranges")
        private String timeZone = "UTC";
	    
	    private int traceCounter;
		
		public FlameGen(CommandLauncher host) {
			this.host = host;
			this.input = new DumpInput(host);
		}
		
		@Override
		public void run() {
			
			try {
				
				TimeZone tz = TimeZone.getTimeZone(timeZone);
				input.setTimeZone(tz);				

				Document template = XmlUtil.parseFromResource("flame_template.html");
				
				FlameTemplateProcessor tproc = new FlameTemplateProcessor(template);
				JsonFlameDataSet dataSet = new JsonFlameDataSet();
				
			    System.out.println("Input files");
			    
			    for(String f: input.sourceFiles()) {
	                System.out.println("  " + f);
			    }
			    System.out.println();
			    
			    dataSet.feed(input.getFilteredReader().morph(new EventMorpher<ThreadSnapshotEvent, ThreadSnapshotEvent>() {
					@Override
					public ThreadSnapshotEvent morph(ThreadSnapshotEvent event) {
						if (event.stackTrace() != null && !event.stackTrace().isEmpty()) {
							++traceCounter;
							return event;
						}
						else {
							return null;
						}
					}			    	
				}));
			    
			    System.out.println(traceCounter + " samples processed");
			    
			    if (traceCounter == 0) {
			    	System.out.println("No data omit report generation");
			    }
			    
			    tproc.setDataSet("fg1", dataSet);
			    
			    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF8"));
			    tproc.generate(writer);
			    writer.close();

			    System.out.println("Generated " + new File(outputFile).getAbsolutePath() + " (" + new File(outputFile).length() + " bytes)");
				
			} catch (Exception e) {
				host.fail("Unexpected error: " + e.toString(), e);
			}			
		}


	}
	
	static class DumpInput extends AbstractThreadDumpSource {
		
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
}
