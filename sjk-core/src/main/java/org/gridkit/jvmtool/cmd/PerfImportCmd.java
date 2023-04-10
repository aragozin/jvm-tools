/**
 * Copyright 2023 Alexey Ragozin
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceWriter;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.util.LinuxPerfImporter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Perf import command.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class PerfImportCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "prfi";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new PrfI(host);
    }

    @Parameters(commandDescription = "Pref import")
    public static class PrfI implements Runnable {

        @ParametersDelegate
        private CommandLauncher host;

        @Parameter(names = {"-i", "--input"}, required = true, description = "Name of file to read perf data, use '-' for stdin")
        private String inputFile;

        @Parameter(names = {"-f", "--filter"}, required = false, description = "Event type filter, default - first event type read")
        private String filter = null;

        @Parameter(names = {"-o", "--output"}, required = true, description = "Name of file to write thread dump")
        private String outputFile;

        @Parameter(names = {"-u", "--upscale"}, required = false, description = "Upscale factor, default 3")
        private double upscale = 3;

        private int traceCounter;
        private StackTraceWriter writer;

        public PrfI(CommandLauncher host) {
            this.host = host;
        }

        @Override
        public void run() {

            try {

                openWriter();

                Reader reader;
                if ("-".equals(inputFile)) {
                    reader = new InputStreamReader(System.in);
                } else {
                    reader = new FileReader(inputFile);
                }

                Iterator<ThreadSnapshot> it = LinuxPerfImporter.parseAndConvert(reader, filter, upscale, 0);

                while (it.hasNext()) {
                    ThreadSnapshot event = it.next();
                    writer.write(event);
                    traceCounter++;
                }

                System.out.println("Produced " + traceCounter + " resampled traces");
                writer.close();

            } catch (Exception e) {
                host.fail("Unexpected error: " + e.toString(), e);
            }
        }

        private void openWriter() throws FileNotFoundException, IOException {
            File file = new File(outputFile);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            writer = StackTraceCodec.newWriter(new FileOutputStream(file));
            System.out.println("Writing to " + file.getAbsolutePath());
        }
    }
}
