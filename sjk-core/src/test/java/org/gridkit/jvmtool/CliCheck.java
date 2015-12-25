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

import junit.framework.Assert;

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

	@Test @Ignore
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
	    exec("hh", "-p", PID, "--dead-young", "-n", "20", "-d", "1s");
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
    public void mx_get_usage_threshold() {
        exec("mx", "-p", PID, "--get", "-all", "--bean", "*:type=MemoryPool,name=PS*", "-f", "CollectionUsageThreshold");
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
    public void mx_get_resetPeakUsage_all() {
        exec("mx", "-p", PID, "--call", "-all", "--bean", "*:type=MemoryPool,*", "-op", "resetPeakUsage");
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
	public void ssa_print() {
	    exec("ssa", "--print", "-f", "target/test.stp");
	}

	@Test
	public void ssa_histo() {
	    exec("ssa", "--histo", "-f", "target/test.stp", "-X");
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
	public void ssa_categorize() {
	    exec("ssa", "--categorize", "-co", "-cf", "src/test/resources/sample-seam-jsf-profile.ctz", "-f", "target/test.stp");
	}

    @Test
    public void ssa_help() {
        exec("ssa", "--ssa-help");
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
