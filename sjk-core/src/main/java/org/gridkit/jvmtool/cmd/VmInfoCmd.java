/**
 * Copyright 2018 Alexey Ragozin
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

import java.util.Map;
import java.util.Properties;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessDetails;
import org.gridkit.lab.jvm.perfdata.JStatData;
import org.gridkit.lab.jvm.perfdata.JStatData.Counter;
import org.gridkit.lab.jvm.perfdata.JStatData.TickCounter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Dump certain details via attach API
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class VmInfoCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "vminfo";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new VmInfo(host);
    }

    @Parameters(commandDescription = "[VMINFO] Dumps various from local VM")
    public static class VmInfo implements Runnable {

        @ParametersDelegate
        private final CommandLauncher host;

// need jcmd support in attach API
        @Parameter(names = {"--flags"}, required = false, description = "Dump XX flags")
        private boolean dumpVmFlags = false;

        @Parameter(names = {"--sysprops"}, required = false, description = "Dump System.getProperties()")
        private boolean dumpSysProps = false;

        @Parameter(names = {"--agentprops"}, required = false, description = "Dump agent properties")
        private boolean dumpAgentProps = false;

        @Parameter(names = {"--perf"}, required = false, description = "Dump perf counters")
        private boolean dumpPerfCounters = false;

        @Parameter(names = {"-p"}, required = true, description = "Target JVM PID")
        private String pid;

        public VmInfo(CommandLauncher host) {
            this.host = host;
        }

        @Override
        public void run() {

            if (!(dumpVmFlags | dumpSysProps | dumpAgentProps | dumpPerfCounters)) {
                host.failAndPrintUsage("No dump option specified");
            }

            JavaProcessDetails jpd = AttachManager.getDetails(Long.parseLong(pid));

            if (dumpVmFlags) {
                StringBuilder sb = new StringBuilder();
                jpd.jcmd("VM.flags -all", sb);
                System.out.println(sb);
            }
            if (dumpSysProps) {
                dumpProps(jpd.getSystemProperties());
            }
            if (dumpAgentProps) {
                dumpProps(jpd.getAgentProperties());
            }
            if (dumpPerfCounters) {
                JStatData jsd = JStatData.connect(Long.parseLong(pid));
                dumpPerf(jsd.getAllCounters());
            }
        }

        private void dumpPerf(Map<String, Counter<?>> allCounters) {
            for(String key: allCounters.keySet()) {
                Counter<?> c = allCounters.get(key);
                if (c instanceof TickCounter) {
                    String val = c.toString();
                    String suffix = "";
                    if (val.lastIndexOf('[') >= 0) {
                        suffix = " " + val.substring(val.lastIndexOf('['));
                    }
                    System.out.println(key + ": " + ((TickCounter)c).getNanos() + " ns" + suffix);
                }
                else {
                    System.out.println(allCounters.get(key));
                }
            }

        }

        private void dumpProps(Properties systemProperties) {
            for(Object k: systemProperties.keySet()) {
                String key = (String) k;
                System.out.println(key + ": " + systemProperties.getProperty(key));
            }
        }
    }
}
