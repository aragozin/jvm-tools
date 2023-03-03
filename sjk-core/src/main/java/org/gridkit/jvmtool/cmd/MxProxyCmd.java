/**
 * Copyright 2017 Alexey Ragozin
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

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.mxproxy.JmxServer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

public class MxProxyCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "mprx";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new MxProxy(host);
    }

    @Parameters(commandDescription = "JMX proxy - expose target process' MBeans for remote access")
    public static class MxProxy implements Runnable {

        @ParametersDelegate
        private CommandLauncher host;

        @ParametersDelegate
        private JmxConnectionInfo jmxConnectionInfo;

        @Parameter(names = {"-b"}, required = true, description = "Bind address - HOST:PORT")
        String bindAddress = null;

        public MxProxy(CommandLauncher host) {
            this.host = host;
            this.jmxConnectionInfo = new JmxConnectionInfo(host);
        }

        @Override
        public void run() {
            try {

                String bhost = "0.0.0.0";
                int bport = 0;

                if (bindAddress != null) {
                    int c = bindAddress.indexOf(':');
                    if (c < 0) {
                        bport = Integer.valueOf(bindAddress);
                    }
                    else {
                        String h = bindAddress.substring(0, c);
                        String p = bindAddress.substring(c + 1);
                        bhost = h;
                        bport = Integer.valueOf(p);
                    }
                }

                if (bport <= 0) {
                    host.fail("Valid bind port required");
                    return;
                }

                System.setProperty("java.rmi.server.hostname", bhost);

                MBeanServerConnection connection = jmxConnectionInfo.getMServer();
                System.out.println("Connected to target JMX endpoint");

                startLocalJMXEndPoint(bhost, bport, connection);
                System.out.println("JMX proxy is running - " + bhost + ":" + bport);
                System.out.println("Interrupt this command to kill proxy");
                // running until interrupted
                while(true) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                host.fail(e.toString(), e);
            }
        }
    }

    private static void startLocalJMXEndPoint(String bindhost, int port, MBeanServerConnection conn) throws MalformedURLException, IOException {

        JmxServer mserver = new JmxServer(conn);
        mserver.setBindHost(bindhost);
        mserver.setPort(port);

        mserver.start();
        System.out.println("Open proxy JMX end point on URI - " + mserver.getJmxUri());

    }
}
