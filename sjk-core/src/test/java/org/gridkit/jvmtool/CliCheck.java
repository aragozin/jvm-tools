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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * JUnit command runner.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CliCheck {

    private static String PID;
    static {
        PID = ManagementFactory.getRuntimeMXBean().getName();
        PID = PID.substring(0, PID.indexOf('@'));
    }
    
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
            ManagementFactory.getPlatformMBeanServer().registerMBean(bean, name);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException(e);
        } catch (MBeanRegistrationException e) {
            throw new RuntimeException(e);
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void help() {
        exec("--help");
    }

    @Test
    public void list_commands() {
        exec("--commands");
    }
    
    @Test
    public void jps() {
        exec("jps");
    }

    @Test
    public void jps_filter_by_prop() {
        System.setProperty("my.prop", "123");
        exec("jps", "-fp", "my.*=123");
    }

    @Test
    public void jps_filter_by_desc() {
        exec("jps", "-fd", "*junit*");
    }

    @Test
    public void jps_print() {
        exec("jps", "-pd", "PID", "MAIN", "Duser.dir");
    }

    @Test
    public void jps_print_flags() {
        exec("jps", "-pd", "PID", "MAIN", "XMaxHeapSize", "XBackgroundCompilation");
    }

    @Test @Ignore
    public void ttop_self() {

        exec("ttop", "-p", PID, "-X");
    }

    @Test @Ignore
    public void ttop_top_N_cpu() {

        exec("ttop", "-p", PID, "-o", "CPU", "-n", "10");
    }

    @Test //@Ignore
    public void ttop_top_N_alloc() {

        exec("ttop", "-p", PID, "-o", "ALLOC", "-n", "10");
    }

    @Test @Ignore
    public void ttop_top_N_filtered() {
        exec("ttop", "-p", PID, "-f", "*RMI*", "-o", "CPU", "-n", "10");
    }

    @Test @Ignore
    public void gc_self() {
        exec("gc", "-p", PID);
    }

    @Test
    public void hh_self() {
        exec("hh", "-p", PID);
    }

    @Test
    public void hh_dead_N_self() {
        exec("hh", "-p", PID, "--dead", "-n", "20");
    }

    @Test
    public void hh_dead_young_N_self() {
        exec("hh", "-p", PID, "--dead-young", "-n", "20", "-d", "10s");
    }

    @Test
    public void hh_young_N_self() {
        exec("hh", "-p", PID, "--young", "-n", "20", "-d", "1s");
    }
    
    @Test
    public void mx_info() {
        exec("mx", "-p", PID, "--info", "--bean", "*:type=HotSpotDiagnostic");
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

        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callSingleStringArg", "-a", "testParam1,testParam2", "-X");
        Assert.assertEquals("testParam1,testParam2", call1arg1);
    }

    @Test
    public void mx_call_method2() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callDoubleStringArg", "-a", "testParam1", "testParam2");
        Assert.assertEquals("testParam1", call2arg1);
        Assert.assertEquals("testParam2", call2arg2);
        
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callDoubleStringArg", "-a", "testParam1", "a,b");
        Assert.assertEquals("testParam1", call2arg1);
        Assert.assertEquals("a,b", call2arg2);
    }

    @Test
    public void mx_call_method3() {
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callStringArrayArg", "-a", "testParam");
        Assert.assertArrayEquals(new String[] {"testParam"}, call3args);
        
        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callStringArrayArg", "-a", "testParam1,testParam2");
        Assert.assertArrayEquals(new String[] {"testParam1", "testParam2"}, call3args);

        exec("mx", "-p", PID, "--call", "--bean", "test:bean=TestBean", "-op", "callStringArrayArg", "-a", "");
        Assert.assertNull(call3args);
    }
    
    @Test
    public void mx_info_ambiguous() {
        fail("mx", "-p", PID, "--info", "--bean", "*:type=GarbageCollector,*");
    }

    @Test
    public void stcap() {
        exec("stcap", "-p", PID, "-o", "target/test.stp");
    }

    @Test
    public void stcap_filter() {
        exec("stcap", "-p", PID, "-m", "javax.*", "-o", "target/test_javax.stp");
    }

    @Test
    public void stcap_rotate() {
        exec("stcap", "-p", PID, "-r", "5000", "-o", "target/test.stp");
    }

    @Test
    public void stcap_rotate_limit() {
        exec("stcap", "-p", PID, "-l", "50000", "-r", "5000", "-o", "target/test.stp");
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
        exec("ssa", "--print", "-f", "target/test.stp");
    }

    @Test
    public void ssa_print_x() {
        exec("ssa", "--print", "-f", "target/test.cap", "-X");
    }

    @Test
    public void ssa_print_trim() {
        exec("ssa", "--print", "-tt", "javax.management.StandardMBean.invoke/+**", "-f", "target/test.stp");
    }

    @Test
    public void ssa_print_thread_name() {
        exec("ssa", "--print", "-tn", "RMI TCP Connection.*", "-f", "target/test.stp");
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
    public void ssa_histo_wild_card() {
        exec("ssa", "--histo", "-f", "target/*.stp", "-X");
    }

    @Test
    public void ssa_histo_with_classes() {
        exec("ssa", "--histo", "-co", "-f", "target/test.stp", "-nc", "IO=java.net.SocketInputStream", "GridKit=org.gridkit", "-X");
    }

    @Test
    public void ssa_histo_masked() {
        exec("ssa", "--histo", "-f", "target/test-masked.all-stp", "-X");
    }
    
    @Test
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
        exec("ssa", "--flame", "-f", "target/test.stp");
    }

    @Test
    public void ssa_flame_with_trim() {
        exec("ssa", "--flame", "-tt", "javax.management.remote.rmi.RMIConnectionImpl.invoke", "-f", "target/test.stp");
    }

    @Test
    public void ssa_flame_rainbow() {
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
        exec("ssa", "--thread-info", "-f", "target/test.stp", "-X");
    }

    @Test
    public void ssa_thread_info_2() {
        exec("ssa", "--thread-info", "-si", "NAME", "FREQ", "FREQ_HM", "GAP_CHM", "TSMIN", "TSMAX", "CPU", "SYS", "-f", "target/test.stp", "-X");
    }

    @Test
    public void ssa_thread_info_3() {
        exec("ssa", "--thread-info", "-si", "NAME8", "ALLOC", "Sock=java.net.SocketInputStream.socketRead0", "-f", "target/test.stp", "-X");
    }

    @Test
    public void ssa_help() {
        exec("ssa", "--ssa-help");
    }

    @Test
    public void flame() {
    	exec("flame", "-f", "target/test.stp", "-o", "target/flame.html");
    }
    
    @Test
    public void dexp_help() {
        exec("dexp", "--help");
    }

    @Test
    public void dexp_tags() {
        exec("dexp", "--tags", "-f",  "target/test.stp");
    }

    @Test
    public void mprx() {
    	exec("mprx", "-p", PID,  "-b", "14000");
    }

    private void exec(String... cmd) {
        SJK sjk = new SJK();
        sjk.suppressSystemExit();
        StringBuilder sb = new StringBuilder();
        sb.append("SJK");
        for(String c: cmd) {
            sb.append(' ').append(escape(c));
        }
        System.out.println(sb);
        Assert.assertTrue(sjk.start(cmd));      
    }

    private void fail(String... cmd) {
        SJK sjk = new SJK();
        sjk.suppressSystemExit();
        StringBuilder sb = new StringBuilder();
        sb.append("SJK");
        for(String c: cmd) {
            sb.append(' ').append(escape(c));
        }
        System.out.println(sb);
        Assert.assertFalse(sjk.start(cmd));     
    }

    private Object escape(String c) {
        if (c.split("\\s").length > 1) {
            return '\"' + c + '\"';
        }
        else {
            return c;
        }
    }   
}
