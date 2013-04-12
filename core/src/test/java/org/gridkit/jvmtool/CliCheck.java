package org.gridkit.jvmtool;

import java.lang.management.ManagementFactory;

import junit.framework.Assert;

import org.junit.Test;

public class CliCheck {

	private static String PID;
	static {
		PID = ManagementFactory.getRuntimeMXBean().getName();
		PID = PID.substring(0, PID.indexOf('@'));
	}
	
	@Test
	public void jps() {
		exec("jps");
	}

	@Test
	public void jps_filter_by_prop() {
		System.setProperty("my.prop", "123");
		exec("jps", "-pf", "my.*=123");
	}

	@Test
	public void jps_filter_by_desc() {
		exec("jps", "-df", "*junit*");
	}

	@Test
	public void jps_print() {
		exec("jps", "-dp", "PID", "MAIN", "Duser.dir");
	}

	@Test
	public void jps_print_flags() {
		exec("jps", "-dp", "PID", "MAIN", "XMaxHeapSize", "XBackgroundCompilation");
	}

	@Test
	public void ttop_self() {
		
		exec("ttop", "-p", PID);
	}

	@Test
	public void ttop_top_N_cpu() {
		
		exec("ttop", "-p", "8420", "-o", "CPU", "-n", "10");
	}

	@Test
	public void ttop_top_N_filtered() {
		exec("ttop", "-p", "8420", "-f", "*RMI*", "-o", "CPU", "-n", "10");
	}

	@Test
	public void gc_self() {
		exec("gc", "-p", PID);
	}

	@Test
	public void mxdump_self() {
		exec("mxdump", "-p", PID);
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

	private Object escape(String c) {
		if (c.split("\\s").length > 1) {
			return '\"' + c + '\"';
		}
		else {
			return c;
		}
	}	
}
