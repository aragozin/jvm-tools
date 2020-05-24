package org.gridkit.jvmtool.cmd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.gridkit.jvmtool.agent.SjkAgent;
import org.gridkit.jvmtool.agent.SjkAgentLocator;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessDetails;

public class SjkAgentHelper {

    private static final String SJK_AGENT_PROP_FALLBACK = SjkAgent.SJK_AGENT_PROP_FALLBACK;

    private static final int JNI_ENOMEM                 = -4;
    private static final int ATTACH_ERROR_BADJAR        = 100;
    private static final int ATTACH_ERROR_NOTONCP       = 101;
    private static final int ATTACH_ERROR_STARTFAIL     = 102;


    public static Properties agentCommand(long pid, Class<?> cmdClass, String args, long timeoutMS) throws IOException {

        JavaProcessDetails pd = AttachManager.getDetails(pid);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        String path = SjkAgentLocator.getJarPath();

        // sending command to load Java agent
        String[] agentArgs = {
            "instrument", // build in to load agent jar
            "false", // absolute
            path + "=" + cmdClass.getName() + ":" + args
        };

        pd.sendAttachCommand("load", agentArgs, bos, timeoutMS);

        String out = new String(bos.toByteArray());
        if (out.startsWith("return code: ")) {
            out = out.substring("return code: ".length());
        }

        int code = Integer.valueOf(out.trim());
        if (code != 0) {
            switch (code) {
                case JNI_ENOMEM:
                    throw new RuntimeException("Insuffient memory");
                case ATTACH_ERROR_BADJAR:
                    throw new RuntimeException(
                        "Agent JAR not found or no Agent-Class attribute");
                case ATTACH_ERROR_NOTONCP:
                    throw new RuntimeException(
                        "Unable to add JAR file to system class path");
                case ATTACH_ERROR_STARTFAIL:
                    throw new RuntimeException(
                        "Agent JAR loaded but agent failed to initialize");
                default :
                    throw new RuntimeException("" +
                        "Failed to load agent - unknown reason: " + code);
            }
        }

        Properties prop = pd.getSystemProperties();
        if ("true".equalsIgnoreCase(prop.getProperty(SJK_AGENT_PROP_FALLBACK))) {
            return prop;
        } else {
            return pd.getAgentProperties();
        }
    }
}
