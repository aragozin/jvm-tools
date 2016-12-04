package org.gridkit.jvmtool.heapdump;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

public class SingletonDetector {

    private RefSet singletons = new RefSet();
    
    public void findSingletons(Heap heap) {
        
        for(JavaClass jc: heap.getAllClasses()) {
            for(FieldValue field: jc.getStaticFieldValues()) {
                if (field instanceof ObjectFieldValue) {
                    long ref = ((ObjectFieldValue)field).getInstanceId();
                    if (ref != 0) {
                        System.out.println(jc.getName() + "#" + field.getField().getName());
                        singletons.set(ref, true);
                    }
                }
            }
        }        
    }
    
    public RefSet getSingletons() {
        return singletons;
    }
}
