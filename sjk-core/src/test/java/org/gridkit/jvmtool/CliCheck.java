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

import java.lang.management.ManagementFactory;

import junit.framework.Assert;

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
	public void hh_self() {
		exec("hh", "-p", PID);
	}

	@Test
	public void hh_dead_N_self() {
		exec("hh", "-p", PID, "--dead", "-n", "20");
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
