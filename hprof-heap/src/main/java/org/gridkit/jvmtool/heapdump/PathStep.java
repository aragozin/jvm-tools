package org.gridkit.jvmtool.heapdump;

import java.util.Iterator;

import org.netbeans.lib.profiler.heap.Instance;

abstract class PathStep {

    public abstract Iterator<Instance> walk(Instance instance);

    public abstract Iterator<Move> track(Instance instance);

    static class Move {

        String pathSpec;
        Instance instance;

        public Move(String pathSpec, Instance instance) {
            super();
            this.pathSpec = pathSpec;
            this.instance = instance;
        }
    }
}
