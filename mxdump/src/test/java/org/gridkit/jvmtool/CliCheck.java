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

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.junit.Test;
import org.junit.Assert;

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
	public void mxdump_self() {
		exec("mxdump", "-p", PID);
	}

	@Test
	public void mxdump_by_query() {
		exec("mxdump", "-p", PID, "-q", "java.lang:type=GarbageCollector,name=*");
	}

	private void exec(String... cmd) {
		CommandLauncher sjk = new CommandLauncher();
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
