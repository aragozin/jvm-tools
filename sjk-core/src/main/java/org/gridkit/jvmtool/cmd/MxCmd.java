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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.MBeanHelper;
import org.gridkit.jvmtool.MTable;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.util.formating.TextTable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class MxCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "mx";
	}

	@Override
	public Runnable newCommand(CommandLauncher host) {
		return new MX(host);
	}

	@Parameters(commandDescription = "[MBean] MBean query and invokation")
	public static class MX implements Runnable {
		
		@ParametersDelegate
		private CommandLauncher host;
		
		@ParametersDelegate
		private JmxConnectionInfo connInfo;

		@Parameter(names={"-b", "--bean"}, required = true, description="MBean name")
		private String mbean;
		
		@Parameter(names={"-f", "--field", "--attribute"}, description="MBean attribute")
		String attrib = null;

        @Parameter(names={"--quiet"}, description="Avoid non-essential output")
        boolean quiet = false;

        @Parameter(names={"--max-col-width"}, description="Table column width threshold for formating tabular data")
		int maxWidth = 40;
		
		@ParametersDelegate
		private CallCmd call = new CallCmd();

        @Parameter(names={"-all", "--allMatched"}, description="Process all matched MBeans")
		private boolean all = false;

		@ParametersDelegate
		private GetCmd get = new GetCmd();

		@ParametersDelegate
		private SetCmd set = new SetCmd();

		@ParametersDelegate
		private InfoCmd info = new InfoCmd();

		public MX(CommandLauncher host) {
			this.host = host;
			this.connInfo = new JmxConnectionInfo(host);
		}

		@Override
		public void run() {
			try {
				List<Runnable> action = new ArrayList<Runnable>();
				if (call.run) {
					action.add(call);
				}
				if (get.run) {
					action.add(get);
				}
				if (set.run) {
					action.add(set);
				}
				if (info.run) {
					action.add(info);
				}
				if (action.isEmpty() || action.size() > 1) {
					host.failAndPrintUsage("You should choose one of --info, --get, --set, --call");
				}
				action.get(0).run();
			} catch (Exception e) {
				host.fail(e.toString(), e);
			}
		}

		private Set<ObjectName> resolveSingleBean(MBeanServerConnection conn) throws Exception {
			ObjectName name = new ObjectName(mbean);
			Set<ObjectName> beans = conn.queryNames(name, null);
			if (beans.isEmpty()) {
				host.fail("MBean not found: " + mbean);
			}
            if (!all && beans.size() > 1) {
                StringBuilder sb = new StringBuilder();
                for(ObjectName n: beans) {
                    sb.append('\n').append(n);
                }
                host.fail("Ambiguous MBean selection. Use '-all' param for process all matched MBeans" + sb.toString());
			}
			return beans;
		}

		class CallCmd implements Runnable {

			@Parameter(names={"-mc", "--call"}, description="Invokes MBean method")
			boolean run;

			@Parameter(names={"-op", "--operation"}, description="MBean operation name to be called")
			String operation = null;

			@Parameter(names={"-a", "--arguments"}, variableArity=true, splitter = Unsplitter.class, description="Arguments for MBean operation invocation")
			List<String> arguments = new ArrayList<String>();

			@Override
			public void run() {
				try {
					if (operation == null) {
						host.failAndPrintUsage("MBean operation name is missing");
					}
					MBeanServerConnection conn = connInfo.getMServer();
                    Set<ObjectName> names = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
                    helper.setFormatingOption(MBeanHelper.FORMAT_TABLE_COLUMN_WIDTH_THRESHOLD, maxWidth);
                    for (ObjectName name : names) {
                        if (!quiet) {
                            System.out.println(name);
                        }
                        System.out.println(helper.invoke(name, operation, arguments.toArray(new String[arguments.size()])));
                    }
				} catch (Exception e) {
					host.fail(e.toString(), e);
				}
			}
		}

		class GetCmd implements Runnable {

			@Parameter(names={"-mg", "--get"}, description="Retrieves value of MBean attribute")
			boolean run;

			@Parameter(names={"--csv"}, description="Used with --get command, result would be formatted as CSV")
			boolean csv; 
			
			@Override
			public void run() {
				try {
					if (attrib == null) {
						host.failAndPrintUsage("MBean operation name is missing");
					}
					MBeanServerConnection conn = connInfo.getMServer();
                    Set<ObjectName> names = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
					if (csv) {
						MTable table = new MTable();
	                    for (ObjectName name : names) {
						    helper.getAsTable(name, attrib, table);
	                    }
	                    if (table.isEmpty()) {
	                    	System.out.println("No data");
	                    }
	                    else {
	                    	TextTable tt = new TextTable();
	                    	table.export(tt);
	                    	System.out.println(TextTable.formatCsv(tt));
	                    }
					}
					else {
						helper.setFormatingOption(MBeanHelper.FORMAT_TABLE_COLUMN_WIDTH_THRESHOLD, maxWidth);
	                    for (ObjectName name : names) {
	                        if (!quiet) {
	                            System.out.println(name);
	                        }
						    System.out.println(helper.get(name, attrib));
	                    }
					}
				} catch (Exception e) {
					host.fail(e.toString(), e);
				}
			}
		}

		class SetCmd implements Runnable {

			@Parameter(names={"-ms", "--set"}, description="Sets value for MBean attribute")
			boolean run;

			@Parameter(names={"-v", "--value"}, description="Value to set to attribute")
			String value = null;

			@Override
			public void run() {
				try {
					if (attrib == null) {
						host.failAndPrintUsage("MBean attribute name is missing");
					}
					if (value == null) {
						host.failAndPrintUsage("Value is required");
					}
					MBeanServerConnection conn = connInfo.getMServer();
                    Set<ObjectName> names = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
                    for (ObjectName name : names) {
                        if (!quiet) {
                            System.out.println(name);
                        }
                        helper.set(name, attrib, value);
                    }
				} catch (Exception e) {
					host.fail(e.toString(), e);
				}
			}
		}

		class InfoCmd implements Runnable {

			@Parameter(names={"-mi", "--info"}, description="Display metadata for MBean")
			boolean run;

			@Override
			public void run() {
				try {
					MBeanServerConnection conn = connInfo.getMServer();
					Set<ObjectName> names = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
                    for (ObjectName name : names) {
                        System.out.println(helper.describe(name));
                    }
				} catch (Exception e) {
					host.fail(e.toString(), e);
				}
			}
		}
	}	
}
