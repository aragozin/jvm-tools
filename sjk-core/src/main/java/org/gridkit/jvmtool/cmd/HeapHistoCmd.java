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

import java.util.concurrent.TimeUnit;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.jvmtool.cli.TimeIntervalConverter;
import org.gridkit.jvmtool.cli.CommandLauncher.CmdRef;
import org.gridkit.lab.jvm.attach.HeapHisto;
import org.gridkit.lab.jvm.perfdata.JStatData;
import org.gridkit.lab.jvm.perfdata.JStatData.LongCounter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Heap histogram command.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class HeapHistoCmd implements CmdRef {

    @Override
    public String getCommandName() {
        return "hh";
    }

    @Override
    public Runnable newCommand(CommandLauncher host) {
        return new Histo(host);
    }

    @Parameters(commandDescription = "[Heap Histo] Prints class histogram, similar to jmap -histo")
    public static class Histo implements Runnable {

        @ParametersDelegate
        private CommandLauncher host;

        @Parameter(names = {"-p", "--pid"}, description = "Process ID", required = true)
        private int pid;

        @Parameter(names = "--live", description = "Live objects histogram")
        private boolean live = false;

        @Parameter(names = "--dead", description = "Dead objects histogram")
        private boolean dead = false;

        @Parameter(names = "--dead-young", description = "Histogram for sample of dead young objects")
        private boolean deadYoung = false;

        @Parameter(names = "--young", description = "Histogram for sample of new objects")
        private boolean young = false;

        @Parameter(names = {"-d", "--sample-depth"}, converter = TimeIntervalConverter.class, description = "Used with --dead-young and --young options. Specific time duration to collect young population.")
        private long youngSampleDepth = 10000;

        @Parameter(names = {"-n", "--top-number"}, description = "Show only N top buckets")
        private int n = Integer.MAX_VALUE;

        public Histo(CommandLauncher host) {
            this.host = host;
        }

        @Override
        public void run() {
            try {
                int x = 0;
                x += live ? 1 : 0;
                x += dead ? 1 : 0;
                x += deadYoung ? 1 : 0;
                x += young ? 1 : 0;
                if (x > 1) {
                    host.failAndPrintUsage("--live, --dead, --young and --deadYoung are mutually exclusive");
                }
//				else if (x < 1) {
//				    host.failAndPrintUsage("--live, --dead, --young, --deadYoung - one of options is required");
//				}

                HeapHisto histo;

                if (live) {
                    histo = HeapHisto.getHistoLive(pid, 300000);
                }
                else if (dead) {
                    histo = HeapHisto.getHistoDead(pid, 300000);
                }
                else if (deadYoung) {
                    histo = collectDeadYoung();
                }
                else if (young) {
                    histo = collectYoung();
                }
                else {
                    // all histo - default behavior
                    histo = HeapHisto.getHistoAll(pid, 300000);
                }

                System.out.println(String.format("%4s %14s%15s  %s", "#", "Instances", "Bytes", "Type"));
                System.out.println(histo.print(n));

            } catch (Exception e) {
                host.fail(e.toString(), e);
            }
        }

        private HeapHisto collectDeadYoung() throws InterruptedException {
            // Force full GC
            long ygc = 0;
            LongCounter youngGcCnt = null;
            try {
                JStatData jsd = JStatData.connect(pid);
                youngGcCnt = (LongCounter) jsd.getAllCounters().get("sun.gc.collector.0.invocations");
                ygc = youngGcCnt == null ? 0 : youngGcCnt.getLong();
            }
            catch(Exception e) {
                // ignore
            }
            HeapHisto.getHistoLive(pid, 3000000);
            System.out.println("Gathering young garbage ...");
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(youngSampleDepth);
            while(System.nanoTime() < deadline) {
                long sleepTime = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                else {
                    break;
                }
            }
            HeapHisto histo = HeapHisto.getHistoDead(pid, 300000);
            if (youngGcCnt != null) {
                if (ygc != youngGcCnt.getLong()) {
                    System.out.println("Warning: one or more young collections have occured during sampling.");
                    System.out.println("Use --sample-depth option to reduce time to sample if needed.");
                }
            }
            if (youngSampleDepth % 1000 == 0) {
                System.out.println("Garbage histogram for last " + (youngSampleDepth/1000) + "s");
            }
            else {
                System.out.println("Garbage histogram for last " + youngSampleDepth + "ms");
            }
            return histo;
        }

        private HeapHisto collectYoung() throws InterruptedException {
            // Force full GC
            long ygc = 0;
            LongCounter youngGcCnt = null;
            try {
                JStatData jsd = JStatData.connect(pid);
                youngGcCnt = (LongCounter) jsd.getAllCounters().get("sun.gc.collector.0.invocations");
                ygc = youngGcCnt == null ? 0 : youngGcCnt.getLong();
            }
            catch(Exception e) {
                // ignore
            }
            HeapHisto retained = HeapHisto.getHistoLive(pid, 3000000);
            System.out.println("Gathering young population ...");
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(youngSampleDepth);
            while(System.nanoTime() < deadline) {
                long sleepTime = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                else {
                    break;
                }
            }
            HeapHisto histo = HeapHisto.getHistoAll(pid, 300000);
            if (youngGcCnt != null) {
                if (ygc != youngGcCnt.getLong()) {
                    System.out.println("Warning: one or more young collections have occured during sampling.");
                    System.out.println("Use --sample-depth option to reduce time to sample if needed.");
                }
            }
            if (youngSampleDepth % 1000 == 0) {
                System.out.println("Garbage histogram for last " + (youngSampleDepth/1000) + "s");
            }
            else {
                System.out.println("Garbage histogram for last " + youngSampleDepth + "ms");
            }
            return HeapHisto.subtract(histo, retained);
        }
    }
}
