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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

import org.gridkit.jvmtool.JvmProcessFilter;
import org.gridkit.jvmtool.JvmProcessPrinter;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;
import org.gridkit.util.formating.GridExportHelper;
import org.gridkit.util.formating.GridSink;
import org.gridkit.util.formating.PrintStreamGridSink;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Java process list command.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ProcListCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "jps";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new JPS(host);
    }

    @Parameters(commandDescription = "[JPS] Enhanced version of JDK's jps tool")
    public static class JPS implements Runnable {

        @ParametersDelegate
        private final CommandLauncher host;

        @ParametersDelegate
        private JvmProcessFilter filter = new JvmProcessFilter();

        @ParametersDelegate
        private JvmProcessPrinter printer;

        @Parameter(names = {"--csv"}, description = "Output in csv format")
        private boolean csv = false;

        @Parameter(names = {"--json"}, description = "Output in JSON format")
        private boolean json = false;

        @Parameter(names = {"--output"}, description = "Output file, use std out if not specified")
        private String outFile = null;

        public JPS(CommandLauncher host) {
            this.host = host;
            this.printer = new JvmProcessPrinter(host);
        }

        @Override
        public void run() {

            List<JavaProcessId> procList;

            PrintStream out = System.out;
            if (outFile != null) {
                File f = new File(outFile);
                if (f.getParentFile() != null) {
                    f.getParentFile().mkdirs();
                }
                try {
                    out = new PrintStream(f);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

            GridExportHelper exportHelper = null;
            GridSink sink = null;

            if (csv || json) {
                if (printer.isDefined()) {
                    sink = exportHelper = new GridExportHelper();
                    printer.describeHeader(sink);
                } else {
                    host.fail("--process-details option is required for cvs/json export");
                }
            } else {
                sink = new PrintStreamGridSink(out);
            }

            filter.prepare();

            if (filter.isDefined() || printer.isDefined()) {
                procList = AttachManager.listJavaProcesses(filter);
            }
            else {
                procList = AttachManager.listJavaProcesses();
            }

            for(JavaProcessId jpid: procList) {
                if (printer.isDefined()) {
                    printer.describe(jpid, sink);
                }
                else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(jpid.getPID()).append('\t');
                    sb.append(jpid.getDescription());
                    System.out.println(sb);
                }
            }

            if (exportHelper != null) {
                if (csv) {
                    exportHelper.exportAsCsv(out);
                } else if (json) {
                    exportHelper.exportAsJson(out);
                }
            }
        }
    }
}
