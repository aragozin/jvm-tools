package org.gridkit.jvmtool.cmd;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.MBeanGCMonitor;
import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;

import com.beust.jcommander.ParametersDelegate;

public class GcRepCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "gc";
	}

	@Override
	public Runnable newCommand(SJK host) {
		return new GcRep(host);
	}
	
	public static class GcRep implements Runnable {

		@SuppressWarnings("unused")
		@ParametersDelegate
		private SJK host;
		
		@ParametersDelegate
		private JmxConnectionInfo conn = new JmxConnectionInfo();

		public GcRep(SJK host) {
			this.host = host;
		}

		@Override
		public void run() {
			
			try {
				MBeanServerConnection mserver = conn.getMServer();
				
				System.out.println("MBean server connected");
				
				MBeanGCMonitor rmon = new MBeanGCMonitor(mserver);
				MBeanGCMonitor pmon = new MBeanGCMonitor(mserver);
				final MBeanGCMonitor fmon = new MBeanGCMonitor(mserver);
				
				Thread freport = new Thread() {
					@Override
					public void run() {
						System.out.println("\nTotal");
						System.out.println(fmon.calculateStats());
					}					
				};

				Runtime.getRuntime().addShutdownHook(freport);

				long interval = 60000;
				long deadline = System.currentTimeMillis() + interval;
				System.out.println("Collecting GC stats ...");
				while(true) {
					while(System.currentTimeMillis() < deadline) {
						String report = rmon.reportCollection();
						if (report.length() > 0) {
							System.out.println(report);
						}
						Thread.sleep(50);
					}
					deadline += interval;
					System.out.println();
					System.out.println(pmon.calculateStats());
					System.out.println();
					pmon = new MBeanGCMonitor(mserver);
					if (System.in.available() > 0) {
						return;
					}
				}
			} catch (Exception e) {
				SJK.fail(e.toString());
			}			
		}
	}
}
