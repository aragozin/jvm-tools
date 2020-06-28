/**
 * Copyright 2014 Alexey Ragozin
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
import java.io.IOException;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.GlobHelper;
import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.TimeIntervalConverter;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceWriter;
import org.gridkit.jvmtool.stacktrace.ThreadDumpSampler;
import org.gridkit.jvmtool.stacktrace.ThreadMXBeanEx;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Stack capture command.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackCaptureCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "stcap";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new StCap(host);
    }

    @Parameters(commandDescription = "[Stack Capture] Dumps stack traces to file for further processing")
    public static class StCap implements Runnable {

        @ParametersDelegate
        private CommandLauncher host;

        @Parameter(names = {"-i", "--sampler-interval"}, converter = TimeIntervalConverter.class, description = "Interval between polling MBeans")
        private long samplerIntervalMS = 0;

        @Parameter(names = {"-f", "--filter"}, description = "Filter threads by name (Java RegEx syntax)")
        private String threadFilter = ".*";

        @Parameter(names = {"-e", "--empty"}, description = "Retain threads without stack trace in dump (ignored by default)")
        private boolean retainEmptyTraces = false;

        @Parameter(names = {"-m", "--match-frame"}, variableArity = true, description = "Frame filter, only traces conatining this string will be included to dump")
        private List<String> frameFilter;

        @Parameter(names = {"-o", "--output"}, required = true, description = "Name of file to write thread dump to")
        private String outputFile;

        @Parameter(names = {"-l", "--limit"}, description = "Target number of traces to collect, once reached command will terminate (0 - unlimited)")
        private long limit = 0;

        @Parameter(names = {"-t", "--timeout"}, converter = TimeIntervalConverter.class, description = "Time until command terminate even without enough traces collected")
        private long timeoutMS = TimeUnit.SECONDS.toMillis(30);

        @Parameter(names = {"-r", "--rotate"}, description = "If specified output file would be rotated every N traces (0 - do not rotate)")
        private long fileLimit = 0;

        @ParametersDelegate
        private JmxConnectionInfo connInfo;

        private ThreadDumpSampler sampler;
        private long traceCounter = 0;
        private long lastRotate = 0;
        private int rotSeg = 0;

        private StackTraceWriter writer;

        public StCap(CommandLauncher host) {
            this.host = host;
            this.connInfo = new JmxConnectionInfo(host);
        }

        @Override
        public void run() {

            try {
                MBeanServerConnection mserver = connInfo.getMServer();
                ThreadMXBean bean = ThreadMXBeanEx.BeanHelper.connectThreadMXBean(mserver);

                sampler = new ThreadDumpSampler();
                sampler.setThreadFilter(threadFilter);

                sampler.connect(bean);

                if (limit == 0) {
                    limit = Long.MAX_VALUE;
                }

                StackTraceWriter proxy = new StackWriterProxy();

                openWriter();
                long deadline = System.currentTimeMillis() + timeoutMS;
                long nextReport = 500;
                while(System.currentTimeMillis() < deadline && traceCounter < limit) {
                    long nextsample = System.currentTimeMillis() + samplerIntervalMS;
                    sampler.collect(proxy);
                    if (traceCounter >= nextReport) {
                        System.out.println("Collected " +traceCounter);
                        while(traceCounter >= nextReport) {
                            nextReport += 500;
                        }
                        checkRotate();
                    }
                    // delay
                    while(nextsample > System.currentTimeMillis()) {
                        long st = nextsample - System.currentTimeMillis();
                        if (st > 0) {
                            Thread.sleep(st);
                        }
                    }
                }

                writer.close();
                System.out.println("Trace dumped: " + traceCounter);

            } catch (Exception e) {
                host.fail("Unexpected error: " + e.toString(), e);
            }
        }

        private void checkRotate() throws FileNotFoundException, IOException {
            if (fileLimit > 0) {
                if (traceCounter - lastRotate > fileLimit) {
                    writer.close();
                    ++rotSeg;
                    lastRotate = traceCounter;
                    openWriter();
                }
            }
        }

        private class StackWriterProxy implements StackTraceWriter {

            private Map<StackFrame, Boolean> elementCache = new HashMap<StackFrame, Boolean>();
            private Matcher[] matchers;

            public StackWriterProxy() {
                if (frameFilter != null) {
                    matchers = new Matcher[frameFilter.size()];
                    for(int i = 0; i != frameFilter.size(); ++i) {
                        matchers[i] = GlobHelper.translate(frameFilter.get(i), ".").matcher("");
                    }
                }
            }

            @Override
            public void write(ThreadSnapshot snap) throws IOException {
                if (snap.stackTrace().isEmpty() && !retainEmptyTraces) {
                    return;
                }
                // test filter
                if (frameFilter != null) {
                    boolean match = false;
                    for(StackFrame e: snap.stackTrace()) {
                        if (match(e)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        return;
                    }
                }
                ++traceCounter;
                writer.write(snap);

            }

            private boolean match(StackFrame e) {
                Boolean cached = elementCache.get(e);
                if (cached == null) {
                    if (elementCache.size() > 4 << 10) {
                        elementCache.clear();
                    }
                    boolean matched = false;
                    for(Matcher m: matchers) {
                        m.reset(e.toString());
                        if (m.lookingAt()) {
                            matched = true;
                            break;
                        }
                    }
                    elementCache.put(e, matched);
                    return matched;
                }
                return cached;
            }

            @Override
            public void close() {
                writer.close();
            }
        }

        private void openWriter() throws FileNotFoundException, IOException {
            if (fileLimit < 1) {
                File file = new File(outputFile);
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                writer = StackTraceCodec.newWriter(new FileOutputStream(file));
                System.out.println("Writing to " + file.getAbsolutePath());
            }
            else {
                int c = outputFile.lastIndexOf('.');
                String pref = c < 0 ? outputFile : outputFile.substring(0, c);
                String suf = c < 0 ? "" : outputFile.substring(c);
                String name = pref + String.format("-%02d", rotSeg) + suf;
                File file = new File(name);
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                writer = StackTraceCodec.newWriter(new FileOutputStream(file));
                System.out.println("Writing to " + file.getAbsolutePath());
            }
        }
    }
}
