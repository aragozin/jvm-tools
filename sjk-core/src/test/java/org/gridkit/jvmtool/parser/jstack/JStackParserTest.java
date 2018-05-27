/**
 * Copyright 2018 Alexey Ragozin
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
package org.gridkit.jvmtool.parser.jstack;

import java.io.StringReader;
import java.lang.management.ManagementFactory;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.junit.Test;

public class JStackParserTest {

    private int pid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        name = name.substring(0, name.indexOf("@"));
        return Integer.parseInt(name);
    }
    
    @Test
    public void test_thread_dump() throws Exception {
        spawnBusyThread("Blocker-1");
        spawnBusyThread("Blocker-2");
        spawnBusyThread("Blocker-3");
        spawnBusyThread("Blocker \"abc\"");

        Thread.sleep(500);
        
    	String[] args = {};
        StringBuilder sb = new StringBuilder();
        AttachManager.getThreadDump(pid(), args, sb, 30000);
        System.out.println(sb);
        
        StringReader sreader = new StringReader(sb.toString());
        JStackDumpParser parser = new JStackDumpParser(sreader);
        
        System.out.println("----------------------------------");        
        System.out.println("Dump parsed: " + parser.isValid());
        System.out.println("JVM: " + parser.getJvmDetails());
        for(String line: parser.getUnparsedContent()) {
        	System.out.println("Unparsed: " + line);
        }
        for(ThreadSnapshotEvent tse: parser.getThreads()) {
        	print(tse);
        }
    }

    private void print(ThreadSnapshotEvent tse) {
    	System.out.println();
		System.out.println("Thread: " + tse.threadName());
		System.out.println(tse.tags());
		System.out.println(tse.counters());
		if (tse.stackTrace() != null) {
			for(StackFrame frame: tse.stackTrace()) {
				System.out.println(" at " + frame);
			}
		}		
	}

    public synchronized void busyCall() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
    }
    
    public void spawnBusyThread(String name) {
        Thread t = new Thread(name) {
            @Override
            public void run() {
                while(true) {
                    busyCall();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }
}
