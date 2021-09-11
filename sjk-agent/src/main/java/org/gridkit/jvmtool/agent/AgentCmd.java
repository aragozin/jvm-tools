package org.gridkit.jvmtool.agent;

import java.lang.instrument.Instrumentation;
import java.util.Properties;

public interface AgentCmd {

    public void start(Properties agentProps, String agentArgs, Instrumentation inst) throws Throwable;

}
