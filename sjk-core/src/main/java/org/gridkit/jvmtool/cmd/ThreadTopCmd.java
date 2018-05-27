/**
 * Copyright 2013 Alexey Ragozin
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.GlobHelper;
import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.MBeanCpuUsageReporter;
import org.gridkit.jvmtool.PerfCounterGcCpuUsageMonitor;
import org.gridkit.jvmtool.PerfCounterSafePointMonitor;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.TimeIntervalConverter;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Thread top command.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ThreadTopCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "ttop";
	}

	@Override
	public Runnable newCommand(CommandLauncher host) {
		return new TTop(host);
	}

	@Parameters(commandDescription = "[Thread Top] Displays threads from JVM process")
	public static class TTop implements Runnable {

		@ParametersDelegate
		private CommandLauncher host;
		
		@Parameter(names = {"-ri", "--report-interval"}, converter = TimeIntervalConverter.class, description = "Interval between CPU usage reports")
		private long reportIntervalMS = TimeUnit.SECONDS.toMillis(10);

		@Parameter(names = {"-si", "--sampler-interval"}, converter = TimeIntervalConverter.class, description = "Interval between polling MBeans")
		private long samplerIntervalMS = 500;
		
		@Parameter(names = {"-n", "--top-number"}, description = "Number of threads to show")
		private int topNumber = 20;
		
		@Parameter(names = {"-o", "--order"}, variableArity = true, description = "Sort order. Value tags: CPU, USER, SYS, ALLOC, NAME")
		private List<String> sortOrder = new ArrayList<String>(Arrays.asList("CPU"));
		
		@Parameter(names = {"-f", "--filter"}, description = "Wild card expression to filter threads by name")
		private String threadFilter;
		
		@Parameter(names = {"-c", "--contention"}, description = "Enable contention monitoring")
		private boolean contentionMon = false;
		
		@ParametersDelegate
		private JmxConnectionInfo connInfo;
		
		public TTop(CommandLauncher host) {
			this.host = host;
			this.connInfo = new JmxConnectionInfo(host);
		}

		@Override
		public void run() {
			
			try {
				MBeanServerConnection mserver = connInfo.getMServer();
				
				final MBeanCpuUsageReporter tmon = new MBeanCpuUsageReporter(mserver);
				if (connInfo.getPID() != null) {
				    try {
    				    long pid = connInfo.getPID();
    				    PerfCounterGcCpuUsageMonitor pm = new PerfCounterGcCpuUsageMonitor(pid);
    				    if (pm.isAvailable()) {
    				        tmon.setGcCpuUsageMonitor(pm);
    				    }
    				    PerfCounterSafePointMonitor sm = new PerfCounterSafePointMonitor(pid);
    				    if (sm.isAvailable()) {
    				        tmon.setSafePointMonitor(sm);
    				    }
//    				    NativeThreadMonitor tm = PlatformProcessInfoProvider.createMonitor(pid);
//    				    if (tm != null) {
//    				        tmon.setNativeThreadMonitor(tm);
//    				    }
				    }
				    catch(Exception e) {
				        // ignore
				    }
				}
				
				tmon.setTopLimit(topNumber);
				tmon.setContentionMonitoringEnabled(contentionMon);
				
				if (threadFilter != null) {
					tmon.setThreadFilter(GlobHelper.translate(threadFilter, "\0"));
				}
				
				if (sortOrder != null) {
					Collections.reverse(sortOrder);
					for(String tag: sortOrder) {
						if ("SYS".equals(tag)) {
							tmon.sortBySysCpu();
						}
						else if ("USER".equals(tag)){
							tmon.sortByUserCpu();
						}
						else if ("CPU".equals(tag)){
							tmon.sortByTotalCpu();
						}
						else if ("ALLOC".equals(tag)){
							tmon.sortByAllocRate();
						}
						else if ("NAME".equals(tag)){
							tmon.sortByThreadName();
						}
						else {
							host.failAndPrintUsage("Invalid order option '" + tag + "'");
						}
					}
				}
				
				long deadline = System.currentTimeMillis() + Math.min(reportIntervalMS, 10 * samplerIntervalMS);
				tmon.report();
				System.out.println("Monitoring threads ...");
				while(true) {
					while(System.currentTimeMillis() < deadline) {
						Thread.sleep(samplerIntervalMS);
						tmon.probe();
					}
					deadline += reportIntervalMS;
					System.out.println();
					System.out.println(tmon.report());
					System.out.println();
					if (System.in.available() > 0) {
						return;
					}
				}
			} catch (Exception e) {
				host.fail("Unexpected error: " + e.toString(), e);
			}			
		}
	}
}
