package org.gridkit.jvmtool.cmd;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.GlobHelper;
import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.MBeanCpuUsageReporter;
import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;
import org.gridkit.jvmtool.TimeIntervalConverter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class ThreadTopCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "ttop";
	}

	@Override
	public Runnable newCommand(SJK host) {
		return new TTop(host);
	}

	public static class TTop implements Runnable {

		@SuppressWarnings("unused")
		@ParametersDelegate
		private SJK host;
		
		@Parameter(names = {"-ri", "--report-interval"}, converter = TimeIntervalConverter.class, description = "Interval between CPU usage reports")
		private long reportIntervalMS = TimeUnit.SECONDS.toMillis(10);

		@Parameter(names = {"-si", "--sampler-interval"}, converter = TimeIntervalConverter.class, description = "Interval between polling MBeans")
		private long samplerIntervalMS = 50;
		
		@Parameter(names = {"-n", "--top-number"}, description = "Number of threads to show")
		private int topNumber = Integer.MAX_VALUE;
		
		@Parameter(names = {"-o", "--order"}, variableArity = true, description = "Sort order. Value tags: CPU, USER, SYS, NAME")
		private List<String> sortOrder;
		
		@Parameter(names = {"-f", "--filter"}, description = "Wild card expression to filter thread by name")
		private String threadFilter;
		
		@ParametersDelegate
		private JmxConnectionInfo connInfo = new JmxConnectionInfo();
		
		public TTop(SJK host) {
			this.host = host;
		}

		@Override
		public void run() {
			
			try {
				MBeanServerConnection mserver = connInfo.getMServer();
				
				final MBeanCpuUsageReporter tmon = new MBeanCpuUsageReporter(mserver);
				
				tmon.setTopLimit(topNumber);
				
				if (threadFilter != null) {
					tmon.setThreadFilter(GlobHelper.translate(threadFilter, "\0"));
				}
				
				if (sortOrder != null) {
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
						else if ("NAME".equals(tag)){
							tmon.sortByThreadName();
						}
						else {
							SJK.failAndPrintUsage("Invalid order option '" + tag + "'");
						}
					}
				}
				
				long deadline = System.currentTimeMillis() + Math.min(reportIntervalMS, 10 * samplerIntervalMS);
				tmon.report();
				System.out.println("Monitoring threads ...");
				while(true) {
					while(System.currentTimeMillis() < deadline) {
						Thread.sleep(samplerIntervalMS);
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
				SJK.fail("Unexpected error: " + e.toString());
			}			
		}
	}
}
