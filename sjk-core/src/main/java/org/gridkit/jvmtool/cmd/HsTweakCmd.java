package org.gridkit.jvmtool.cmd;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.gridkit.jvmtool.agent.HotspotInternalMBeanEnabler;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class HsTweakCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "hs";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new HsTweak(host);
    }

    @Parameters(commandDescription = "Hotspot JVM tweaks")
    public static class HsTweak implements Runnable {

        @Parameter(names = {"-p"}, required = true, description = "Target JVM PID")
        private long pid;

        @Parameter(names = {"--enable-hotspot-mbean"}, required = true, description = "Activate Hotspot Internal MBean")
        private boolean enableHotspotMBean = false;

        @ParametersDelegate
        private CommandLauncher host;

        public HsTweak(CommandLauncher host) {
            this.host = host;
        }

        @Override
        public void run() {
            try {
                if (host.isVerbose()) {
                    SjkAgentHelper.enableTrace(true);
                }

                if (!enableHotspotMBean) {
                    host.fail("No option provided. At least one of following options is required:"
                            + "\n  --enable-hotspot-mbean");
                }

                if (enableHotspotMBean) {
                    SjkAgentHelper.agentCommand(pid, HotspotInternalMBeanEnabler.class, "", TimeUnit.SECONDS.toMillis(30));
                }

            } catch (IOException e) {
                host.fail("", e);
            }
        }
    }
}
