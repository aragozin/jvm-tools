package org.gridkit.jvmtool.heapdump;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;

class ArrayIndexStep extends PathStep {

    private final int index;

    public ArrayIndexStep(int index) {
        this.index = index;
    }

    @Override
    public Iterator<Instance> walk(Instance instance) {
        if (instance instanceof ObjectArrayInstance) {
            ObjectArrayInstance array = (ObjectArrayInstance) instance;
            if (index < 0) {
                return array.getValues().iterator();
            }
            else {
                if (array.getLength() > index) {
                    return Collections.singleton(array.getValues().get(index)).iterator();
                }
                else {
                    return Collections.<Instance>emptyList().iterator();
                }
            }
        }
        else {
            return Collections.<Instance>emptyList().iterator();
        }
    }

    @Override
    public Iterator<Move> track(Instance instance) {
        if (instance instanceof ObjectArrayInstance) {
            ObjectArrayInstance array = (ObjectArrayInstance) instance;
            if (index < 0) {
                List<Move> result = new ArrayList<Move>();
                int n = 0;
                for(Instance i: array.getValues()) {
                    result.add(new Move("[" + n + "]", i));
                    ++n;
                }
                return result.iterator();
            }
            else {
                if (array.getLength() > index) {
                    return Collections.singleton(new Move("[" + index + "]", array.getValues().get(index))).iterator();
                }
                else {
                    return Collections.<Move>emptyList().iterator();
                }
            }
        }
        else {
            return Collections.<Move>emptyList().iterator();
        }
    }

    @Override
    public String toString() {
        return index < 0 ? "[*]" : "[" + index + "]";
    }
}
