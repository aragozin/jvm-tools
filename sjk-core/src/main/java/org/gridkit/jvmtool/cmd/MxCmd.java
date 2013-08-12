package org.gridkit.jvmtool.cmd;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class MxCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "mx";
	}

	@Override
	public Runnable newCommand(SJK host) {
		return new MX(host);
	}

	public static class MX implements Runnable {
		
		@SuppressWarnings("unused")
		@ParametersDelegate
		private SJK host;
		
		@ParametersDelegate
		private JmxConnectionInfo connInfo = new JmxConnectionInfo();

		@Parameter(names={"-b", "--bean"}, description="MBean name")
		private String mbean;
		
		private CallCmd call;
		
		private GetCmd get;
		
		private SetCmd set;
		
		private InfoCmd info;
		
		public MX(SJK host) {
			this.host = host;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
		}

		class CallCmd implements Runnable {
			
			@Parameter(names={"-mc", "--call"}, description="Invokes MBean method")
			boolean run;

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		}
		
		class GetCmd implements Runnable {
			
			@Parameter(names={"-mg", "--get"}, description="Retrieves value of MBean attribute")
			boolean run;
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		}
		
		class SetCmd implements Runnable {
		
			@Parameter(names={"-ms", "--set"}, description="Sets value for MBean attribute")
			boolean run;

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		}
		
		class InfoCmd implements Runnable {
			
			@Parameter(names={"-mi", "--info"}, description="Display metadata for MBean")
			boolean run;
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		}
	}	
}
