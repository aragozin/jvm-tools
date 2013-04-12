package org.gridkit.jvmtool.cmd;

import java.util.List;

import org.gridkit.jvmtool.JvmProcessFilter;
import org.gridkit.jvmtool.JvmProcessPrinter;
import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;

import com.beust.jcommander.ParametersDelegate;

public class ProcListCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "jps";
	}

	@Override
	public Runnable newCommand(SJK host) {
		return new JPS(host);
	}
	
	public static class JPS implements Runnable {

		@SuppressWarnings("unused")
		@ParametersDelegate
		private final SJK host;

		@ParametersDelegate
		private JvmProcessFilter filter = new JvmProcessFilter();
		
		@ParametersDelegate
		private JvmProcessPrinter printer = new JvmProcessPrinter();
		
		public JPS(SJK host) {
			this.host = host;
		}

		@Override
		public void run() {
			
			List<JavaProcessId> procList; 
			
			filter.prepare();
			
			if (filter.isDefined() || printer.isDefined()) {
				procList = AttachManager.listJavaProcesses(filter);
			}
			else {
				procList = AttachManager.listJavaProcesses();
			}
			
			for(JavaProcessId jpid: procList) {
				if (printer.isDefined()) {
					System.out.println(printer.describe(jpid));
				}
				else {
					StringBuilder sb = new StringBuilder();
					sb.append(jpid.getPID()).append('\t');
					sb.append(jpid.getDescription());
					System.out.println(sb);
				}
			}
		}
	}
}
