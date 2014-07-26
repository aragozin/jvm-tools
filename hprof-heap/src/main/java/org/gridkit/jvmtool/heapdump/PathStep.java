package org.gridkit.jvmtool.heapdump;

import java.util.Iterator;

import org.netbeans.lib.profiler.heap.Instance;

abstract class PathStep {

    public abstract Iterator<Instance> walk(Instance instance);

}
