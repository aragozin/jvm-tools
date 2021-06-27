/**
 * Copyright 2013 Alexey Ragozin
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

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.MBeanGCMonitor;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * GC reporter command.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class GcRepCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "gc";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new GcRep(host);
    }

    @Parameters(commandDescription = "[Print GC] Print GC log like information for remote process")
    public static class GcRep implements Runnable {

        @ParametersDelegate
        private CommandLauncher host;

        @ParametersDelegate
        private JmxConnectionInfo conn;

        public GcRep(CommandLauncher host) {
            this.host = host;
            this.conn = new JmxConnectionInfo(host);
        }

        @Override
        public void run() {

            try {
                MBeanServerConnection mserver = conn.getMServer();

                System.out.println("MBean server connected");

                MBeanGCMonitor rmon = new MBeanGCMonitor(mserver);
                MBeanGCMonitor pmon = new MBeanGCMonitor(mserver);
                final MBeanGCMonitor fmon = new MBeanGCMonitor(mserver);

                Thread freport = new Thread() {
                    @Override
                    public void run() {
                        System.out.println("\nTotal");
                        System.out.println(fmon.calculateStats());
                    }
                };

                Runtime.getRuntime().addShutdownHook(freport);

                long interval = 60000;
                long deadline = System.currentTimeMillis() + interval;
                System.out.println("Collecting GC stats ...");
                while(true) {
                    while(System.currentTimeMillis() < deadline) {
                        String report = rmon.reportCollection();
                        if (report.length() > 0) {
                            System.out.println(report);
                        }
                        Thread.sleep(50);
                    }
                    deadline += interval;
                    System.out.println();
                    System.out.println(pmon.calculateStats());
                    System.out.println();
                    pmon = new MBeanGCMonitor(mserver);
                    if (System.in.available() > 0) {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                // silent exit by interruption
            } catch (Exception e) {
                host.fail(e.toString());
            }
        }
    }
}
