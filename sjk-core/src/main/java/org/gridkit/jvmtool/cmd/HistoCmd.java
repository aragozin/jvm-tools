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

import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;
import org.gridkit.lab.jvm.attach.HeapHisto;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Heap histogram command.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class HistoCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "hh";
	}

	@Override
	public Runnable newCommand(SJK host) {
		return new Histo(host);
	}
	
	@Parameters(commandDescription = "[Heap Histo] Prints class histogram, similar to jmap -histo")
	public static class Histo implements Runnable {

		@SuppressWarnings("unused")
		@ParametersDelegate
		private SJK host;
		
		@Parameter(names = {"-p", "--pid"}, description = "Process ID")
		private int pid;
		
		@Parameter(names = "--live", description = "Live objects histogram")
		private boolean live = false;

		@Parameter(names = "--dead", description = "Dead objects histogram")
		private boolean dead = false;

		@Parameter(names = {"-n", "--top-number"}, description = "Show only N top buckets")
		private int n = Integer.MAX_VALUE;

		public Histo(SJK host) {
			this.host = host;
		}

		@Override
		public void run() {
			try {
				if (live && dead) {
					SJK.failAndPrintUsage("--live and --dead are mutually exclusive");
				}
				
				HeapHisto histo;
				
				if (live) {
					histo = HeapHisto.getHistoLive(pid, 300000);
				}
				else if (dead) {
					histo = HeapHisto.getHistoDead(pid, 300000);
				}
				else {
					histo = HeapHisto.getHistoAll(pid, 300000);
				}
				
				System.out.println(histo.print(n));

			} catch (Exception e) {
				SJK.fail(e.toString(), e);
			}
		}
	}
}
