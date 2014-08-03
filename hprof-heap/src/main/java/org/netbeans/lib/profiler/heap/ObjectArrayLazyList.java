package org.netbeans.lib.profiler.heap;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ObjectArrayLazyList extends AbstractObjectArrayLazyList<Instance> {

    public ObjectArrayLazyList(HprofHeap h, HprofByteBuffer buf, int len, long off) {
        super(h, buf, len, off);
    }

    @Override
    public Instance get(int index) {
        return getInstance(index);
    }

}
