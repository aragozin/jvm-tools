package org.gridkit.jvmtool.heapdump;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

class FieldStep extends PathStep {

    private final String fieldName;

    FieldStep(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Iterator<Instance> walk(Instance instance) {
        List<Instance> result = new ArrayList<Instance>();
        for(FieldValue fv: instance.getFieldValues()) {
            if (fieldName == null || fieldName.equals(fv.getField().getName())) {
                if (fv instanceof ObjectFieldValue) {
                    result.add(((ObjectFieldValue) fv).getInstance());
                }
            }
        }

        return result.iterator();
    }

    @Override
    public String toString() {
        return fieldName == null ? "*" :  fieldName;
    }
}
