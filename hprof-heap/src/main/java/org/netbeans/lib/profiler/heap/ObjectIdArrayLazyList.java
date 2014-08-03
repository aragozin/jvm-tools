package org.netbeans.lib.profiler.heap;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ObjectIdArrayLazyList extends AbstractObjectArrayLazyList<Long> {

    public ObjectIdArrayLazyList(HprofHeap h, HprofByteBuffer buf, int len, long off) {
        super(h, buf, len, off);
    }

    @Override
    public Long get(int index) {
        return getInstanceID(index);
    }

}
