/**
 * Copyright 2014 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.netbeans.lib.profiler.heap;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * All heap instances iterator, scanning heap returning every instance.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class HprofInstanceIterator implements Iterator<Instance> {

    private final HprofHeap heap;
    private final TagBounds allInstanceDumpBounds;
    private final long[] pointer;
    private final HprofByteBuffer dumpBuffer;
    private Instance nextInstance;


    public HprofInstanceIterator(HprofHeap heap, long start) {
        this.heap = heap;
        allInstanceDumpBounds = heap.getAllInstanceDumpBounds();
        pointer = new long[]{start};
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
        while(pointer[0] < allInstanceDumpBounds.endOffset) {

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

            if (jc == null) {
                // Dump details of broken instances
//                System.err.print("Bad heap entry, missing class ref(#" + instanceClassId + ")");
//                try {
//                    if (tag == HprofHeap.INSTANCE_DUMP) {
//                        jc = (ClassDump) heap.getJavaClassByName("java.lang.Object");
//                        Instance instance = new InstanceDump(jc, start);
//                        System.err.print(" ID:" + instance.getInstanceId());
//                    } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
//                        jc = (ClassDump) heap.getJavaClassByName("java.lang.Object[]");
//                        ObjectArrayInstance instance = new ObjectArrayDump(jc, start);  
//                        System.err.print(" ID:" + instance.getInstanceId() + " array length: " + instance.getLength());
//                    } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
//                        System.err.print(" PRIMITIVE_ARRAY");
//                    }
//                }
//                catch(Exception e) {
//                    System.err.println(" {" + e.toString() + "}");
//                }
//                System.err.println(" dump size: " + (pointer[0] - start));
                continue;
            }
            
            Instance instance = null;
            if (tag == HprofHeap.INSTANCE_DUMP) {
                instance = new InstanceDump(jc, start);
            } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
                instance = new ObjectArrayDump(jc, start);   
            } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                instance = new PrimitiveArrayDump(jc, start);
            } else {
                // ignore
                continue;
            }

            return instance;
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    static class AsIterable implements Iterable<Instance> {

        private final HprofHeap heap;
        private final long start;

        public AsIterable(HprofHeap heap) {
            this.heap = heap;
            this.start = heap.getAllInstanceDumpBounds().startOffset;
        }

        public AsIterable(HprofHeap heap, long start) {
            this.heap = heap;
            this.start = start;
        }

        @Override
        public Iterator<Instance> iterator() {
            return new HprofInstanceIterator(heap, start);
        }
    }
}
