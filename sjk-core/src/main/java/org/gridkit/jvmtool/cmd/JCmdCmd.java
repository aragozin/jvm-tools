package org.gridkit.jvmtool.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.MBeanHelper;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;

import com.beust.jcommander.IVariableArity;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class JCmdCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "jcmd";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new JCmdRunner(host);
    }

    @Parameters(commandDescription = "jcmd JMX command")
    public static class JCmdRunner implements Runnable, IVariableArity {

        @ParametersDelegate
        private CommandLauncher host;

        @ParametersDelegate
        private JmxConnectionInfo connInfo;

        @Parameter(names = {"-c", "--cmd"}, required = true, variableArity = true, splitter = Unsplitter.class, description = "jcmd like command" )
        private List<String> command = new ArrayList<String>();

        public JCmdRunner(CommandLauncher host) {
            this.host = host;
            this.connInfo = new JmxConnectionInfo(host);
        }

        @Override
        public void run() {
            try {
                if (host.isVerbose()) {
                    SjkAgentHelper.enableTrace(true);
                }

                if (command.isEmpty()) {
                    command = Arrays.asList("help");
                }

                String cmd = command.get(0);
                List<String> args = command.subList(1, command.size());

                ObjectName bname = new ObjectName("com.sun.management:type=DiagnosticCommand");
                MBeanServerConnection conn = connInfo.getMServer();
                MBeanHelper helper = new MBeanHelper(conn);
                MBeanOperationInfo[] ops;
                try {
                    ops = conn.getMBeanInfo(bname).getOperations();
                } catch (InstanceNotFoundException e) {
                    host.fail("jcmd MBean is not supported by JVM");
                    return;
                }
                boolean found = false;
                for (MBeanOperationInfo mop: ops) {
                    String op = mop.getName();
                    String opName = getOpName(mop);
                    if (cmd.equals(opName)) {
                        found = true;

                        String result;

                        if (mop.getSignature().length == 0) {
                            if (args.isEmpty()) {
                                result = helper.invoke(bname, op, new String[0]);
                            } else {
                                host.fail("command '" + cmd + "' doesn't take any arguments");
                                return;
                            }
                        } else {
                            MBeanParameterInfo[] meta = mop.getSignature();
                            String[] sig = new String[meta.length];
                            for(int i = 0; i != meta.length; ++i) {
                                sig[i] = meta[i].getType();
                            }
                            result = (String) conn.invoke(bname, op, new Object[] {args.toArray(new String[0])}, sig);
                        }

                        System.out.println(result);

                        break;
                    }
                }
                if (!found) {
                    host.fail("Operation is not available at target JVM '" + cmd + "'");
                    return;
                }


            } catch (Exception e) {
                host.fail("", e);
            }
        }

        private String getOpName(MBeanOperationInfo mop) {
            String opname = (String) mop.getDescriptor().getFieldValue("dcmd.name");
            return opname;
        }

        @Override
        public int processVariableArity(String optionName, String[] options) {
            return options.length;
        }
    }
}
