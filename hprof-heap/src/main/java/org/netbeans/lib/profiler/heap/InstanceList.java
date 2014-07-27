package org.netbeans.lib.profiler.heap;

import java.util.AbstractList;

class InstanceList extends AbstractList<Instance> {

    private final Heap heap;
    private final long[] instances;

    public InstanceList(Heap heap, long[] instances) {
        this.heap = heap;
        this.instances = instances;
    }

    @Override
    public Instance get(int index) {
        return heap.getInstanceByID(instances[index]);
    }

    @Override
    public int size() {
        return instances.length;
    }

}
