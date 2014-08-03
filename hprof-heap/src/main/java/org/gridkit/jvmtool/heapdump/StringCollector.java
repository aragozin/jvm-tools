package org.gridkit.jvmtool.heapdump;

import org.gridkit.jvmtool.heapdump.HeapHistogram.ClassRecord;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

public class StringCollector {

    private RefSet strings = new RefSet();
    private long count;
    private long totalSize;

    public StringCollector() {
    }

    public void collect(Heap heap) {
        collect(heap, null);
    }

    public void collect(Heap heap, InstanceCallback callback) {
        JavaClass string = heap.getJavaClassByName("java.lang.String");
        RefSet arrays = new RefSet();
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
        for(Long id: arrays.ones()) {
            totalSize += heap.getInstanceByID(id).getSize();
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
}
