package org.gridkit.jvmtool.heapdump;

import java.util.Iterator;

import org.netbeans.lib.profiler.heap.Instance;

/** This is ** mark. Direct walking is not supported */
class AnyPathStep extends PathStep {

    @Override
    public Iterator<Instance> walk(Instance instance) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return "**";
    }
}
