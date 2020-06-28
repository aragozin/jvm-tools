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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.lab.jvm.attach.SysLogger;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Dump certain details via attach API
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class MxPingCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "mxping";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new MxPingInfo(host);
    }

    @Parameters(commandDescription = "[MXPING] Verify JMX connection to target JVM")
    public static class MxPingInfo implements Runnable {

        @ParametersDelegate
        private final CommandLauncher host;

        @ParametersDelegate
        private JmxConnectionInfo connInfo;

        public MxPingInfo(CommandLauncher host) {
            this.host = host;
            connInfo = new JmxConnectionInfo(host);
        }

        @Override
        public void run() {

            SysLogger.DEBUG.setTarget(System.out);
            SysLogger.INFO.setTarget(System.out);

            connInfo.setDiagMode(true);

            RuntimeMXBean rtinfo = ManagementFactory.getRuntimeMXBean();
            System.out.println("SJK is running on: " + rtinfo.getVmName() + " " + rtinfo.getVmVersion() + " (" + rtinfo.getVmVendor() + ")");
            System.out.println("Java home: " + System.getProperty("java.home"));

            MBeanServerConnection mserver = connInfo.getMServer();
            ObjectName on;
            try {
                on = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
            }
            catch(Exception e) {
                host.fail("", e);
                return;
            }
            RuntimeMXBean rtbean;
            try {
                rtbean = JMX.newMBeanProxy(mserver, on, RuntimeMXBean.class);
            }
            catch(Exception e) {
                host.fail("Faield to access remote Runtime MBean", e);
                return;
            }
            System.out.println("Remote VM: " + rtbean.getVmName() + " " + rtbean.getVmVersion() + " (" + rtbean.getVmVendor() + ")");
        }
    }
}
