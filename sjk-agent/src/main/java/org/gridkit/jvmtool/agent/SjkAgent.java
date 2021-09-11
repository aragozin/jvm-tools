package org.gridkit.jvmtool.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Properties;

public class SjkAgent {

    public static final String SJK_AGENT_LAST_ERROR = "sjk.agent.last.error";
    public static final String SJK_AGENT_PROP_FALLBACK = "sjk.agent.prop.fallback";

    private static Properties getAgentProperties() {

        Class<?> vmSup = null;
        try {
            // try classic VM support
            vmSup = Class.forName("sun.misc.VMSupport");
        } catch (ClassNotFoundException e) {
            // ignore
        }
        if (vmSup == null) {
            try {
                vmSup = Class.forName("jdk.internal.vm.VMSupport");
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }

        Properties props = null;
        if (vmSup != null) {
            try {
                Method m = vmSup.getMethod("getAgentProperties");
                m.setAccessible(true);
                props = (Properties)m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
                // ignore
            }
        }

        if (props != null) {
            return props;
        }
        else {
            System.err.println("Failed to get agent properties, falling back to sys properties");
            System.getProperties().put(SJK_AGENT_PROP_FALLBACK, "true");
            return System.getProperties();
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        try {
            ModuleHack.extendAccess(inst);
        } catch (Throwable e) {
            // this is expected to fail on Java 8 and below
        }

        Properties agentProps = getAgentProperties();

        agentProps.remove(SJK_AGENT_LAST_ERROR);

        String[] args = agentArgs.split(":");
        if (args.length > 0 && args[0].trim().length() > 0) {
            String cmdClass = args[0];
            AgentCmd cmd = null;
            try {
                cmd = (AgentCmd) Class.forName(cmdClass).newInstance();
            } catch (Exception e) {
                failInstantiate(agentProps, cmdClass, e);
                return;
            }
            try {
                cmd.start(agentProps, agentArgs, inst);
            } catch (Error e) {
                throw e;
            } catch (Throwable e) {
                agentProps.setProperty(SJK_AGENT_LAST_ERROR, "Command error " + e.toString());
                return;
            }

        } else {
            agentProps.setProperty(SJK_AGENT_LAST_ERROR, "Arguments are missing");
            return;
        }
    }

    private static void failInstantiate(Properties agentProps, String cmdClass,Exception e) {
        agentProps.setProperty(SJK_AGENT_LAST_ERROR, "Failed to instantiate class '" + cmdClass + "' - " + e.toString());
    }
}
