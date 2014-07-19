package org.netbeans.lib.profiler.heap;

import java.util.Iterator;
import java.util.NoSuchElementException;

class HprofInstanceIterator implements Iterator<Instance> {

    private final HprofHeap heap;
    private final TagBounds allInstanceDumpBounds;
    private final long[] pointer;
    private final HprofByteBuffer dumpBuffer;
    private Instance nextInstance;


    public HprofInstanceIterator(HprofHeap heap) {
        this.heap = heap;
        allInstanceDumpBounds = heap.getAllInstanceDumpBounds();
        pointer = new long[]{allInstanceDumpBounds.startOffset};
        dumpBuffer = heap.dumpBuffer;
        nextInstance = seek();
    }

    @Override
    public boolean hasNext() {
        return nextInstance != null;
    }

    @Override
    public Instance next() {
        if (nextInstance == null) {
            throw new NoSuchElementException();
        }
        else {
            Instance i = nextInstance;
            nextInstance = seek();
            return i;
        }
    }

    Instance seek() {
        if (pointer[0] >= allInstanceDumpBounds.endOffset) {
            return null;
        }
        else {
            long start = pointer[0];
            int classIdOffset = 0;
            long instanceClassId = 0L;
            int tag = heap.readDumpTag(pointer);
            int idSize = dumpBuffer.getIDSize();

            if (tag == HprofHeap.INSTANCE_DUMP) {
                classIdOffset = idSize + 4;
            } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
                classIdOffset = idSize + 4 + 4;
            } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                byte type = dumpBuffer.get(start + 1 + idSize + 4 + 4);
                instanceClassId = heap.getClassDumpSegment().getPrimitiveArrayClass(type).getJavaClassId();
            }

            if (classIdOffset != 0) {
                instanceClassId = dumpBuffer.getID(start + 1 + classIdOffset);
            }

            ClassDump jc = (ClassDump) heap.getJavaClassByID(instanceClassId);

            Instance instance = null;
            if (tag == HprofHeap.INSTANCE_DUMP) {
                instance = new InstanceDump(jc, start);
            } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
                instance = new ObjectArrayDump(jc, start);
            } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                instance = new PrimitiveArrayDump(jc, start);
            } else {
                // ignore
            }

            return instance;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    static class AsIterable implements Iterable<Instance> {

        private final HprofHeap heap;

        public AsIterable(HprofHeap heap) {
            this.heap = heap;
        }

        @Override
        public Iterator<Instance> iterator() {
            return new HprofInstanceIterator(heap);
        }
    }
}
