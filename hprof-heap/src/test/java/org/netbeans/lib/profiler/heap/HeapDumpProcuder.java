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
package org.netbeans.lib.profiler.heap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.gridkit.lab.jvm.attach.HeapDumper;
import org.junit.Test;

public class HeapDumpProcuder {

    private static int PID;
    static {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        PID = Integer.valueOf(pid.substring(0, pid.indexOf('@')));
    }

    private static String HEAP_DUMP_PATH = "target/dump/test.dump";
    private static String HEAP_DUMP_GZ_PATH = "target/dump/test.dump.gz";

    public static File getHeapDump() {
        File file = new File(HEAP_DUMP_PATH);
        if (!file.exists()) {
            System.out.println("Generating heap dump: " + HEAP_DUMP_PATH);
            holder.initTestHeap();
            System.out.println(HeapDumper.dumpLive(PID, HEAP_DUMP_PATH, 120000));
        }
        return file;
    }

    public static File getCompressedHeapDump() throws IOException {
        File file = new File(HEAP_DUMP_GZ_PATH);
        if (!file.exists()) {
            System.out.println("Generating compressing heap dump: " + HEAP_DUMP_GZ_PATH);
            FileInputStream fis = new FileInputStream(getHeapDump());
            FileOutputStream fos = new FileOutputStream(file);
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
            byte[] buf = new byte[64 << 10];
            int n = 0;
            while(true) {
                n = fis.read(buf);
                if (n < 0) {
                    break;
                }
                gzos.write(buf, 0, n);
            }
            gzos.close();
            fis.close();
            fos.close();
        }
        return file;
    }
    
    // Called manually from IDE to clean cached dump
    @Test
    public void cleanDump() {
        new File(HEAP_DUMP_PATH).delete();
        new File(HEAP_DUMP_GZ_PATH).delete();
    }

    static Holder holder = new Holder();
    
    static class Holder { 
    
	    List<DummyA> dummyA = new ArrayList<DummyA>();
	    List<DummyB> dummyB = new ArrayList<DummyB>();
	    DummyC dummyC = new DummyC();
	    DummyD dummyD = new DummyD();
	    {
	        dummyD.nestedArray = new DummyD.Sub[2];
	        dummyD.nestedArray[1] = new DummyD.Sub();
	        dummyD.nestedArray[1].value = "somevalue";
	    }
	    
	    DummyP[] dummyP = {
	    		new DummyP("A", "X"),
	    		new DummyP("B", null),
	    		new DummyP("C", 1),
	    		new DummyP("D", null),
	    		new DummyP("E", new Object[0]),
	    };
	
	    public void initTestHeap() {
	
	        for(int i = 0; i != 50; ++i) {
	            dummyA.add(new DummyA());
	        }
	
	        for(int i = 0; i != 50; ++i) {
	            DummyB dmb = new DummyB();
	            dmb.seqNo = String.valueOf(i);
	            for(int j = 0; j != i; ++j) {
	                dmb.list.add(String.valueOf(j));
	                dmb.map.put("k" + String.valueOf(j), "v" + String.valueOf(j));
	            }
	            dummyB.add(dmb);
	        }
	    }
    }
}
