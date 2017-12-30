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
package org.gridkit.jvmtool.heapdump;

import org.gridkit.jvmtool.heapdump.HeapHistogram.ClassRecord;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.IllegalInstanceIDException;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

public class StringCollector {

    private RefSet strings = new RefSet();
    private RefSet arrays = new RefSet();
    private long count;
    private long totalSize;

    public StringCollector() {
    }

    public void collect(Heap heap) {
        collect(heap, null);
    }

    public void collect(Heap heap, InstanceCallback callback) {
        JavaClass string = heap.getJavaClassByName("java.lang.String");
        for(Instance i : heap.getAllInstances()) {
            if (i.getJavaClass() == string) {
                strings.set(i.getInstanceId(), true);
                for(FieldValue fv: i.getFieldValues()) {
                    if ("value".equals(fv.getField().getName())) {
                        arrays.set(((ObjectFieldValue)fv).getInstanceId(), true);
                    }
                }
                ++count;
                totalSize += i.getSize();
            }
            if (callback != null) {
                callback.feed(i);
            }
        }
        arrays.set(0, false); // skip null reference
        for(Long id: arrays.ones()) {
            try {
                Instance s = heap.getInstanceByID(id);
                if (s != null) {
			totalSize += s.getSize();
                }
                else {
			System.err.println("Missing instance #" + id);
                }
            }
            catch(IllegalInstanceIDException e) {
                // ignore
            }
        }
    }

    public long getInstanceCount() {
        return count;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public RefSet getInstances() {
        return strings;
    }

    public ClassRecord asClassRecord() {
        ClassRecord cr = new ClassRecord(String.class.getName() + " (retained)");
        cr.instanceCount = count;
        cr.totalSize = totalSize;
        return cr;
    }

    public String toString() {
        return "strings: " + totalSize + " (" + count + ")";
    }

    public boolean containsInstance(Instance i) {
        return strings.get(i.getInstanceId()) || arrays.get(i.getInstanceId());
    }
}
