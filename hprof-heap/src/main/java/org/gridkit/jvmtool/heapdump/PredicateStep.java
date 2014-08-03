package org.gridkit.jvmtool.heapdump;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

public class PredicateStep extends PathStep {

    private PathStep[] path;
    private FieldStep lastStep;
    private String matcher;

    public PredicateStep(PathStep[] path, String matcher) {
        if (path.length > 0 && path[path.length - 1] instanceof FieldStep) {
            this.path = Arrays.copyOf(path, path.length - 1);
            this.lastStep = (FieldStep) path[path.length - 1];
        }
        else {
            this.path = path;
        }
        this.matcher = matcher;
    }

    @Override
    public Iterator<Instance> walk(Instance instance) {
        for(Instance i: HeapPath.collect(instance, path)) {
            if (lastStep != null) {
                String fname = lastStep.getFieldName();
                for(FieldValue fv: i.getFieldValues()) {
                    if ((fname == null && fv.getField().isStatic())
                            || (fname.equals(fv.getField().getName()))) {
                        Object obj;
                        if (fv instanceof ObjectFieldValue) {
                            obj = HeapWalker.valueOf(((ObjectFieldValue) fv).getInstance());
                        }
                        else {
                            // have to use this as private package API is used behind scene
                            obj = i.getValueOfField(fv.getField().getName());
                        }
                        if (!(obj instanceof Instance)) {
                            String str = String.valueOf(obj);
                            if (str.equals(matcher)) {
                                return Collections.singleton(instance).iterator();
                            }
                        }
                    }
                }
            }
            else {
                Object obj = HeapWalker.valueOf(i);
                if (!(obj instanceof Instance)) {
                    String str = String.valueOf(obj);
                    if (str.equals(matcher)) {
                        return Collections.singleton(instance).iterator();
                    }
                }
            }
        }
        return Collections.<Instance>emptyList().iterator();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(PathStep step: path) {
            sb.append(step).append(", ");
        }
        if (lastStep != null) {
            sb.append(lastStep).append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("=").append(matcher).append("]");
        return sb.toString();
    }
}
