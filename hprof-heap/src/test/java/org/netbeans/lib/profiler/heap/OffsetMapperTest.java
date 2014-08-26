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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

public class OffsetMapperTest {

    Heap heap;

    @Before
    public void initHeap() throws FileNotFoundException, IOException {
        heap = HeapFactory.createHeap(HeapDumpProcuder.getHeapDump());
    }

    @Test
    public void verify_heap_offsets_hashed() {
        HashSet<Long> instances = new HashSet<Long>();
        for(Instance i: heap.getAllInstances()) {
            instances.add(i.getInstanceId());
        }

        HeapOffsetMap map = new HeapOffsetMap((HprofHeap)heap);

        for(long id: instances) {
            long offs = map.offset(id);
            Assert.assertTrue(offs > 0);
            Assert.assertEquals(id, readID(offs));
        }

        System.out.println("Heap instances: " + instances.size());
    }

    @Test
    public void verify_heap_offsets_ordered() {
        Set<Long> instances = new TreeSet<Long>();
        for(Instance i: heap.getAllInstances()) {
            instances.add(i.getInstanceId());
        }

        HeapOffsetMap map = new HeapOffsetMap((HprofHeap)heap);

        for(long id: instances) {
            long offs = map.offset(id);
            Assert.assertTrue(offs > 0);
            Assert.assertEquals(id, readID(offs));
        }

        System.out.println("Heap instances: " + instances.size());
    }

//    @Test
//    public void verify_heap_offsets_for_classes() {
//        Set<Long> instances = new TreeSet<Long>();
//        for(JavaClass i: heap.getAllClasses()) {
//            instances.add(i.getJavaClassId());
//        }
//
//        HeapOffsetMap map = new HeapOffsetMap((HprofHeap)heap);
//
//        for(long id: instances) {
//            long offs = map.offset(id);
//            Assert.assertTrue(offs > 0);
//            Assert.assertEquals(id, readID(offs));
//        }
//
//        System.out.println("Heap classes: " + instances.size());
//    }

    private long readID(long offset) {
        long[] pointer = new long[]{offset};
        HprofHeap heap = (HprofHeap) this.heap;
        TagBounds bounds = heap.getAllInstanceDumpBounds();

        while(pointer[0] < bounds.endOffset) {
            long ptr = pointer[0];
            int tag = heap.readDumpTag(pointer);

            if (   tag == HprofHeap.INSTANCE_DUMP
                || tag == HprofHeap.OBJECT_ARRAY_DUMP
                || tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                return heap.dumpBuffer.getID(ptr + 1);
            }
        }
        return -1;
    }
}
