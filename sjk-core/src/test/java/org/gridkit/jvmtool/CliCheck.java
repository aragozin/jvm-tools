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
package org.gridkit.jvmtool;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.gridkit.sjk.test.console.StopCommandAfter;
import org.gridkit.sjk.test.console.junit4.CliTestRule;
import org.gridkit.sjk.test.console.junit4.ConsoleRule;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * JUnit command runner.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CliCheck {

    private static final boolean JAVA_6 = System.getProperty("java.version").startsWith("1.6");

    private static final boolean JAVA_7 = System.getProperty("java.version").startsWith("1.7");


    private static String PID;
    static {
        PID = ManagementFactory.getRuntimeMXBean().getName();
        PID = PID.substring(0, PID.indexOf('@'));
    }

    @Rule
    public CliTestRule cli = new CliTestRule(SJK.class);

    public ConsoleRule stdOut = cli.out;
    public ConsoleRule stdErr = cli.err;

    private String call1arg1;
    private String call2arg1;
    private String call2arg2;
    private String[] call3args;

    {
        try {
            ObjectName name = new ObjectName("test:bean=TestBean");
            DummyMBean bean = new DummyMBean() {

                @Override
                public void callStringArrayArg(String[] args) {
                    call3args = args;
                }

                @Override
                public void callSingleStringArg(String arg) {
                    call1arg1 = arg;
                }

                @Override
                public void callDoubleStringArg(String arg1, String arg2) {
                    call2arg1 = arg1;
                    call2arg2 = arg2;
                }
            };
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
            } catch (Exception e) {
                // ignore
            }
            ManagementFactory.getPlatformMBeanServer().registerMBean(bean, name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException(e);
        } catch (MBeanRegistrationException e) {
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void help() {
        exec("--help");
        stdOut.line("Usage: <main class> [options] [command] [command options]");
    }

    @Test
    public void list_commands() {
        exec("--commands");
        stdOut.lineContains("dexp");
        stdOut.lineContains("flame");
        stdOut.lineContains("gc");
        stdOut.lineContains("hh");
        stdOut.lineContains("hs");
        stdOut.lineContains("jfr2json");
        stdOut.lineContains("jps");
        stdOut.lineContains("mprx");
        stdOut.lineContains("mx");
        stdOut.lineContains("mxping");
        stdOut.lineContains("ssa");
        stdOut.lineContains("stcap");
        stdOut.lineContains("stcpy");
        stdOut.lineContains("ttop");
        stdOut.lineContains("vminfo");
    }

    @Test
    public void jps() {
        exec("jps");

        // some PID something expected
        stdOut.lineEx("[0-9]+\\s.*");
    }

    static String KEY = String.valueOf(System.currentTimeMillis());
    static {
        System.setProperty("my.prop", KEY);
    }


    @Test
    public void jps_filter1_by_prop() {
        exec("jps", "-fp", "my.*=" + KEY);

        // some PID something expected
        stdOut.skip();
        stdOut.lineEx("[0-9]+\\s.*(junit|surefire).*");
    }

    @Test
    public void jps_filter2_by_desc() {
        exec("jps", "-fd", "*");

        // some PID something expected
        stdOut.skip();
        stdOut.lineEx("[0-9]+\\s.*(junit|surefire).*");
    }

    @Test
    public void jps_print() {
        exec("jps", "-pd", "PID", "MAIN", "Duser.dir");
    }

    @Test
    public void jps_print_flags() {
        exec("jps", "-pd", "PID", "MAIN", "XMaxHeapSize", "XBackgroundCompilation");
    }

    @Test @StopCommandAfter(10)
    public void ttop_self() {

        exec("ttop", "-p", PID, "-X");
        stdOut.skip();
        stdOut.line("Monitoring threads ...");
        stdOut.skip(1);
        stdOut.lineContains("Process summary");
        stdOut.lineContains("process cpu=");
        stdOut.lineContains("application cpu=");
        stdOut.lineContains("other: cpu=");
        stdOut.lineContains("thread count:");
        stdOut.lineContains("GC time=");
        stdOut.lineContains("heap allocation rate");
        stdOut.lineContains("safe point rate");
        stdOut.lineContains("safe point sync time");
    }

    @Test @StopCommandAfter(10)
    public void ttop_top_N_cpu() {

        exec("ttop", "-p", PID, "-o", "CPU", "-n", "10");
    }

    @Test @StopCommandAfter(10)
    public void ttop_top_N_alloc() {

        exec("ttop", "-p", PID, "-o", "ALLOC", "-n", "10");
    }

    @Test @StopCommandAfter(10)
    public void ttop_top_N_filtered() {
        exec("ttop", "-p", PID, "-f", "*RMI*", "-o", "CPU", "-n", "10");
    }

    @Test @StopCommandAfter(10)
    public void gc_self() {
        exec("gc", "-p", PID);
        stdOut.line("MBean server connected");
        stdOut.line("Collecting GC stats ...");
    }

    @Test
    public void hh_self() {
        exec("hh", "-p", PID);
        stdOut.skip();
        stdOut.lineContains("#", "Instances", "Bytes", "Type");
        stdOut.skip();
        stdOut.lineContains("Total");
    }

    @Test
    public void hh_dead_N_self() {
        exec("hh", "-p", PID, "--dead", "-n", "20");
        stdOut.skip();
        stdOut.lineContains("#", "Instances", "Bytes", "Type");
        stdOut.skip();
        stdOut.lineContains("20:");
        stdOut.lineContains("Total");
    }

    @Test
    public void hh_dead_young_N_self() {
        exec("hh", "-p", PID, "--dead-young", "-n", "20", "-d", "10s");
        stdOut.skip();
        stdOut.line("Gathering young garbage ...");
        stdOut.skip();
        stdOut.line("Garbage histogram for last 10s");
        stdOut.lineContains("#", "Instances", "Bytes", "Type");
        stdOut.skip();
        stdOut.lineContains("20:");
        stdOut.lineContains("Total");
    }

    @Test
    public void hh_young_N_self() {
        exec("hh", "-p", PID, "--young", "-n", "20", "-d", "1s");

        stdOut.skip();
        stdOut.line("Garbage histogram for last 1s");
        stdOut.skip();
        stdOut.lineContains("20:");
        stdOut.lineContains("Total");
    }

    @Test
    public void mx_info() {
        exec("mx", "-p", PID, "--info", "--bean", "*:type=HotSpotDiagnostic");

        stdOut.line("com.sun.management:type=HotSpotDiagnostic");
        stdOut.line("sun.management.HotSpotDiagnostic");
        stdOut.line(" - Information on the management interface of the MBean");
        stdOut.line(" (A) DiagnosticOptions : CompositeData[]");
        stdOut.line(" (O) dumpHeap(String p0, boolean p1) : void");
        stdOut.line(" (O) getVMOption(String p0) : CompositeData");
        stdOut.line(" (O) setVMOption(String p0, String p1) : void");
    }

    @Test
    public void mx_info_all() {
        exec("mx", "-p", PID, "--info", "-all", "--bean", "*:type=MemoryPool,*");
    }

    @Test
    public void mx_get_diagnostic_ops() {
        exec("mx", "-p", PID, "--get", "--bean", "*:type=HotSpotDiagnostic", "-f", "DiagnosticOptions");
    }

    @Test
    public void mx_get_diagnostic_ops_csv() {
        exec("mx", "-p", PID, "--get", "--csv", "--bean", "*:type=HotSpotDiagnostic", "-f", "DiagnosticOptions");
    }

    @Test
    public void mx_get_system_properties_csv() {
        exec("mx", "-p", PID, "--get", "--csv", "--bean", "*:type=Runtime", "-f", "SystemProperties");
    }

    @Test
    public void mx_get_usage_threshold() {
        exec("mx", "-p", PID, "--get", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "CollectionUsageThreshold");
    }

    @Test
    public void mx_get_memory_pool_usage_csv() {
        exec("mx", "-p", PID, "--get", "--csv", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "Usage");
    }

    @Test
    public void mx_get_memory_pool_usage_peakusage() {
        exec("mx", "-p", PID, "--get", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "Usage", "-f", "PeakUsage");
    }

    @Test
    public void mx_get_memory_pool_usage_peakusage_csv() {
        exec("mx", "-p", PID, "--get", "--csv", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "Usage", "-f", "PeakUsage");

        stdOut.line("committed,init,max,used");
    }

    @Test
    public void mx_get_memory_pool_usage_peakusage_csv_add_bean_name() {
        exec("mx", "-p", PID, "--get", "--csv", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "Usage,PeakUsage", "--add-mbean-name");

        stdOut.line("MBean,Attribute,committed,init,max,used");
    }

    @Test
    public void mx_get_memory_pool_usage_peakusage_csv_with_projection() {
        exec("mx", "-p", PID, "--get", "--csv", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "Usage,PeakUsage", "--col-list", "Attribute,-,init,used,committed");

        stdOut.line("Attribute,-,init,used,committed");
        stdOut.lineEx("(Usage|PeakUsage),,\\d+,\\d+,\\d+");
    }

    @Test
    public void mx_set_threading_alloc() {
        exec("mx", "-p", PID, "--set", "--bean", "*:type=Threading", "-f", "ThreadAllocatedMemoryEnabled", "-v", "true");
    }

    @Test
    public void mx_set_usage_threshold() {
        exec("mx", "-p", PID, "--set", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "CollectionUsageThreshold", "-v", "1");
    }

    @Test
    public void mx_get_thread_dump() {
        exec("mx", "-p", PID, "--call", "--bean", "*:type=Threading", "-op", "dumpAllThreads", "-a", "true", "true");
    }

    @Test
    public void mx_get_thread_dump_quiet_wide() {
        exec("mx", "-p", PID, "--quiet", "--max-col-width", "80", "--call", "--bean", "*:type=Threading", "-op", "dumpAllThreads", "-a", "true", "true");
    }

    @Test
    public void mx_get_resetPeakUsage_all() {
        exec("mx", "-p", PID, "--call", "-all", "--bean", "*:type=MemoryPool,*", "-op", "resetPeakUsage");
    }

    @Test
    public void mx_call_method1() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callSingleStringArg", "-a", "testParam");
        Assert.assertEquals("testParam", call1arg1);
    }

    @Test
    public void mx_call_method1a() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callSingleStringArg", "-a", "testParam1,testParam2", "-X");
        Assert.assertEquals("testParam1,testParam2", call1arg1);
    }

    @Test
    public void mx_call_method2() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callDoubleStringArg", "-a", "testParam1", "testParam2");
        Assert.assertEquals("testParam1", call2arg1);
        Assert.assertEquals("testParam2", call2arg2);
    }

    @Test
    public void mx_call_method2a() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callDoubleStringArg", "-a", "testParam1", "a,b");
        Assert.assertEquals("testParam1", call2arg1);
        Assert.assertEquals("a,b", call2arg2);
    }

    @Test
    public void mx_call_method3() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callStringArrayArg", "-a", "testParam");
        Assert.assertArrayEquals(new String[] {"testParam"}, call3args);
    }

    @Test
    public void mx_call_method3a() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callStringArrayArg", "-a", "testParam1,testParam2");
        Assert.assertArrayEquals(new String[] {"testParam1", "testParam2"}, call3args);
    }

    @Test
    public void mx_call_method3b() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callStringArrayArg", "-a", "");
        Assert.assertNull(call3args);
    }

    @Test
    public void mx_info_ambiguous() {
        fail("mx", "-p", PID, "--info", "--bean", "*:type=GarbageCollector,*");

        stdErr.line("Ambiguous MBean selection. Use '-all' param for process all matched MBeans");
        stdErr.lineStarts("java.lang:type=GarbageCollector,name=");
        stdErr.lineStarts("java.lang:type=GarbageCollector,name=");
    }

    public void ensureTestStp() {
        if (!new File("target/test.stp").isFile()) {
            Thread testThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    while(true) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
            testThread.setName("TestThread");
            testThread.start();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            exec("stcap", "-p", PID, "-o", "target/test.stp");
            testThread.interrupt();
            stdOut.verify();
            stdOut.clean();
            stdErr.verify();
            stdErr.clean();
        }
    }

    public void ensureTestStp500() {
        if (!new File("target/test500.stp").isFile()) {
            exec("stcap", "-p", PID, "-l", "500", "-o", "target/test500.stp");
            stdOut.verify();
            stdErr.verify();
            stdOut.clean();
            stdErr.clean();
        }
    }

    @Test
    public void stcap() {
        exec("stcap", "-p", PID, "-o", "target/test-stcap.stp");
    }

    @Test
    public void stcap_filter() {
        exec("stcap", "-p", PID, "-m", "javax.*", "-o", "target/test_javax.stp");
    }

    @Test
    public void stcap_rotate() {
        exec("stcap", "-p", PID, "-r", "5000", "-o", "target/test-stcap.stp");
    }

    @Test
    public void stcap_rotate_limit() {
        exec("stcap", "-p", PID, "-l", "50000", "-r", "5000", "-o", "target/test-stcap.stp");
    }

    @Test
    public void stcpy() {
        exec("stcpy", "-X", "-i", "target/*.stp", "-o", "target/test.all-stp");
    }

    @Test
    public void stcpy_abs() {
        exec("stcpy", "-X", "-i", new File("target").getAbsolutePath().replace('\\', '/') + "/*.stp", "-o", "target/test.all-stp");
    }

    @Test
    public void stcpy_mask() {
        exec("stcpy", "-X", "--mask", "org.gridkit:com.acme", "-i", "target/test.all-stp", "-o", "target/test-masked.all-stp");
    }

    @Test
    public void stcpy_tt() {
        exec("stcpy", "-X", "--mask", "org.gridkit:com.acme", "-i", "target/test.all-stp", "-tt", "javax.management.StandardMBean.invoke/+**", "-o", "target/test-trimmed.all-stp");
    }

    @Test
    public void stcpy_tr() {
        exec("stcpy", "-X", "-tr", "00:05-00:10", "-i", "target/test.all-stp", "-tt", "javax.management.StandardMBean.invoke/+**", "-o", "target/test-time-rnaged.all-stp");
    }

    @Test
    public void ssa_print() {
        ensureTestStp500();
        exec("ssa", "--print", "-f", "target/test500.stp");
    }

    @Test
    public void ssa_print_partial_file_list() {
        ensureTestStp500();
        exec("ssa", "--print", "-f", "target/test500.stp", "target/test.no-such-file");
    }

    @Test @Ignore
    public void ssa_print_some() {
        exec("ssa", "--print", "-f", "target/test-trimmed.all-stp");
    }

    @Test
    public void ssa_print_fail() {
        fail("ssa", "--print", "-f", "target/test.no-such-file", "-X");
    }

    @Test
    public void ssa_print_trim() {
        exec("ssa", "--print", "-tt", "javax.management.StandardMBean.invoke/+**", "-f", "target/test.stp");
    }

    @Test
    public void ssa_print_thread_name() {
        ensureTestStp500();
        exec("ssa", "--print", "-tn", "RMI TCP Connection.*", "-f", "target/test500.stp");
    }

    @Test
    public void ssa_print_time_range() {
        exec("ssa", "--print", "-tr", "02:11-02:12", "-tn", "RMI TCP Connection.*", "-f", "target/test.stp");
    }

    @Test
    public void ssa_histo() {
        exec("ssa", "--histo", "-f", "target/test.stp", "-X");
    }

    @Test
    public void ssa_histo_term_sort() {
        exec("ssa", "--histo", "-f", "target/test.stp", "--by-term", "-X");
    }

    @Test
    public void ssa_histo_term_sort2() {
        exec("ssa", "--histo", "-f", "target/test.stp", "--sort", "TERM", "-X");
    }

    @Test
    public void ssa_histo_occur_sort() {
        exec("ssa", "--histo", "-f", "target/test.stp", "--sort", "OCCUR", "-X");
    }

    @Test
    public void ssa_histo_freq_sort() {
        exec("ssa", "--histo", "-f", "target/test.stp", "--sort", "FREQ", "-X");
    }

    @Test
    public void ssa_histo_wild_card() {
        exec("ssa", "--histo", "-f", "target/*.stp", "-X");
    }

    @Test
    public void ssa_histo_with_classes() {
        exec("ssa", "--histo", "-co", "-f", "target/test.stp", "-nc", "IO=java.net.SocketInputStream", "GridKit=org.gridkit", "-X");
    }

    @Test @Ignore
    public void ssa_histo_masked() {
        exec("ssa", "--histo", "-f", "target/test-masked.all-stp", "-X");
    }

    @Test @Ignore
    public void ssa_histo2() {
        exec("ssa", "--histo", "-f", "target/test_javax.stp", "-X");
    }

    @Test
    public void ssa_histo_with_filter() {
        exec("ssa", "--histo", "-tf", "javax.management.remote.rmi.RMIConnectionImpl.invoke", "-f", "target/test.stp");
    }

    @Test
    public void ssa_histo_with_trim() {
        exec("ssa", "--histo", "-tt", "javax.management.remote.rmi.RMIConnectionImpl.invoke/+**", "-f", "target/test.stp");
    }

    @Test
    public void ssa_histo_with_trim2() {
        exec("ssa", "--histo", "-tt", "javax.management.remote.rmi.RMIConnectionImpl.invoke", "-f", "target/test.stp");
    }

    @Test
    public void ssa_histo_with_trim3() {
        exec("ssa", "--histo", "-tf", "**!**.jdbc", "-tt", "org.hibernate", "-f", "../sjk-stacktrace/src/test/resources/jboss-10k.std");
    }

    @Test
    public void ssa_flame() {
        ensureTestStp();
        exec("ssa", "--flame", "-f", "target/test.stp");
    }

    @Test
    public void ssa_flame_with_trim() {
        ensureTestStp();
        exec("ssa", "--flame", "-tt", "javax.management.remote.rmi.RMIConnectionImpl.invoke", "-f", "target/test.stp");
    }

    @Test
    public void ssa_flame_rainbow() {
        ensureTestStp();
        exec("ssa", "--flame", "-f", "target/test.stp");
    }

    @Test
    public void ssa_categorize() {
        exec("ssa", "--categorize", "-co", "-cf", "src/test/resources/sample-seam-jsf-profile.ctz", "-f", "../sjk-stacktrace/src/test/resources/jboss-10k.std");
    }

    @Test
    public void ssa_categorize_nc() {
        exec("ssa", "--categorize", "-nc", "JDBC=**.jdbc", "-f", "../sjk-stacktrace/src/test/resources/jboss-10k.std");
    }

    @Test
    public void ssa_thread_info() {
        ensureTestStp();
        exec("ssa", "--thread-info", "-f", "target/test.stp", "-X");
        stdOut.lineEx("Name\\s+Count\\s+On CPU\\s+Alloc\\s+RUNNABLE\\s+Native");
        stdOut.skip();
        stdOut.lineStarts("TestThread");
    }

    @Test
    public void ssa_thread_info_2() {
        ensureTestStp();
        exec("ssa", "--thread-info", "-si", "NAME", "FREQ", "FREQ_HM", "GAP_CHM", "TSMIN", "TSMAX", "CPU", "SYS", "-f", "target/test.stp", "-X");
        stdOut.lineEx("Name\\s+Freq[.]\\s+\\QFreq. (1/HM)\\E\\s+Gap CHM\\s+First time\\s+Last time\\s+On CPU\\s+System");
        stdOut.skip();
        stdOut.lineStarts("TestThread");
    }

    @Test
    public void ssa_thread_info_3() {
        ensureTestStp();
        exec("ssa", "--thread-info", "-si", "NAME8", "ALLOC", "Sock=java.net.SocketInputStream.socketRead0", "-f", "target/test.stp", "-X");
        stdOut.lineEx("Name\\s+Alloc\\s+Sock");
        stdOut.skip();
        stdOut.lineStarts("TestThre ");
    }

    @Test
    public void ssa_thread_info_csv() {
        ensureTestStp();
        exec("ssa", "--thread-info", "-co", "-f", "target/test.stp", "-X");
        stdOut.line("Name,Count,\"On CPU\",Alloc,RUNNABLE,Native");
        stdOut.skip();
        stdOut.lineStartsEx("TestThread,([^,^\\n]+),([^,^\\n]+),([^,^\\n]+),([^,^\\n]+),([^,^\\n]+)",
                null, "\"0.0%\"", "\"0/s\"", "\"0.0%\"", "\"100.0%\"");
    }

    @Test
    public void ssa_thread_info_csv_numeric() {
        ensureTestStp();
        exec("ssa", "--thread-info", "-co", "--numeric", "-f", "target/test.stp", "-X");
        stdOut.line("Name,Count,\"On CPU\",Alloc,RUNNABLE,Native");
        stdOut.skip();
        stdOut.lineStartsEx("TestThread,([^,^\\n]+),([^,^\\n]+),([^,^\\n]+),([^,^\\n]+),([^,^\\n]+)",
                null, "0", "0", "0", "1");
    }

    @Test
    public void ssa_help() {
        exec("ssa", "--ssa-help");
    }

    @Test
    public void flame() {
        ensureTestStp();
        exec("flame", "-f", "target/test.stp", "-o", "target/flame.html");
    }

    @Test
    public void dexp_help() {
        exec("dexp", "--help");
    }

    @Test
    public void dexp_tags() {
        ensureTestStp();
        exec("dexp", "--tags", "-f",  "target/test.stp");
    }

    @Test @StopCommandAfter(5)
    public void mprx() {
        exec("mprx", "-p", PID,  "-b", "34000");

        stdOut.line("Connected to target JMX endpoint");
        stdOut.line("Open proxy JMX end point on URI - service:jmx:rmi://0.0.0.0:34000/jmxrmi");
        stdOut.line("JMX proxy is running - 0.0.0.0:34000");
    }

    @Test
    public void vminfo_sysprops() {
        exec("vminfo", "-p", PID, "--sysprops");
    }

    @Test
    public void vminfo_agentprops() {
        exec("vminfo", "-p", PID, "--agentprops");
    }

    @Test
    public void vminfo_perf() {
        exec("vminfo", "-p", PID, "--perf");
    }

    @Test
    public void vminfo_flags() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("vminfo", "-p", PID, "--flags");
    }

    @Test
    public void hs_hsmbean() {
        exec("hs", "-p", PID, "--enable-hotspot-mbean", "-X");
    }

    @Test
    public void jcmd_help() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("jcmd", "-p", PID, "-X", "-c", "help");
    }

    @Test
    public void jcmd_help_gc_run() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("jcmd", "-p", PID, "-X", "-c", "help", "GC.run");
    }

    @Test
    public void jcmd_gc_run() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("jcmd", "-p", PID, "-X", "-c", "GC.run");
    }

    @Test
    public void jcmd_vm_uptime() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("jcmd", "-p", PID, "-X", "-c", "VM.uptime", "-date");
    }

    @Test
    public void jcmd_vm_command_line() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("jcmd", "-p", PID, "-X", "-c", "VM.command_line");
    }

    @Test
    public void jcmd_jfr_check() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("jcmd", "-p", PID, "-X", "-c", "JFR.check", "verbose=true");
    }

    @Test
    public void jcmd_thread_print() {
        Assume.assumeTrue(!JAVA_6 && !JAVA_7);
        exec("jcmd", "-p", PID, "-X", "-c", "Thread.print", "-l");
    }

    private void exec(String... cmd) {
        cli.exec(cmd);
    }

    private void fail(String... cmd) {
        cli.fail(cmd);
    }
}
