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

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridkit.jvmtool.heapdump.HeapWalker.stringValue;
import static org.gridkit.jvmtool.heapdump.HeapWalker.walkFirst;

import java.util.ArrayList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.gridkit.jvmtool.heapdump.HeapWalker;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public abstract class BaseHeapTest {

    public abstract Heap getHeap();

    @Test
    public void verify_DummyA_via_classes() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyA.class.getName());
        int n = 0;
        for(Instance i : jclass.getInstances()) {
            ++n;
            assertThat(i.getFieldValues().size()).isEqualTo(1);
            FieldValue fv = i.getFieldValues().get(0);
            assertThat(fv).isInstanceOf(ObjectFieldValue.class);
            Instance ii = ((ObjectFieldValue)fv).getInstance();
            assertThat(ii).isInstanceOf(PrimitiveArrayInstance.class);
        }

        assertThat(n).isEqualTo(50);
    }

    @Test
    public void verify_DummyA_via_scan() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyA.class.getName());
        int n = 0;
        for(Instance i : heap.getAllInstances()) {
            if (i.getJavaClass() == jclass) {
                ++n;
                assertThat(i.getFieldValues().size()).isEqualTo(1);
                FieldValue fv = i.getFieldValues().get(0);
                assertThat(fv).isInstanceOf(ObjectFieldValue.class);
                Instance ii = ((ObjectFieldValue)fv).getInstance();
                assertThat(ii).isInstanceOf(PrimitiveArrayInstance.class);
            }
        }

        assertThat(n).isEqualTo(50);
    }

    @Test
    public void verify_heap_walker_for_dummyB() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyB.class.getName());
        int n = 0;
        for(Instance i : jclass.getInstances()) {
            ++n;
            int no = Integer.valueOf(stringValue(walkFirst(i, "seqNo")));
            SortedSet<String> testSet = new TreeSet<String>();
            for(Instance e :  HeapWalker.walk(i, "list.elementData[*]")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            assertThat(testSet).isEqualTo(testSet("", no));

            testSet.clear();
            for(Instance e :  HeapWalker.walk(i, "map.table[*].key")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            // some entries may be missing due to hash collisions
            assertThat(testSet("k", no).containsAll(testSet)).isTrue();

            testSet.clear();
            for(Instance e :  HeapWalker.walk(i, "map.table[*].value")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            // some entries may be missing due to hash collisions
            assertThat(testSet("v", no).containsAll(testSet)).isTrue();
        }

        assertThat(n).isEqualTo(50);
    }

    @Test
    public void verify_heap_walker_for_dummyB_over_map() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyB.class.getName());
        int n = 0;
        for(Instance i : jclass.getInstances()) {
            ++n;
            int no = Integer.valueOf(stringValue(walkFirst(i, "seqNo")));
            SortedSet<String> testSet = new TreeSet<String>();
            for(Instance e :  HeapWalker.walk(i, "list.elementData[*]")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            assertThat(testSet).isEqualTo(testSet("", no));

            testSet.clear();
            for(Instance e :  HeapWalker.walk(i, "map?entrySet.key")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            assertThat(testSet).isEqualTo(testSet("k", no));

            testSet.clear();
            for(Instance e :  HeapWalker.walk(i, "map?entrySet.value")) {
                if (e != null) {
                    testSet.add(stringValue(e));
                }
            }
            assertThat(testSet).isEqualTo(testSet("v", no));

            if (testSet.size() > 5) {
                assertThat(HeapWalker.valueOf(i, "map?entrySet[key=k3].value")).isEqualTo("v3");
            }
        }

        assertThat(n).isEqualTo(50);
    }

    @SuppressWarnings("unused")
    @Test
    public void verify_heap_walker_for_array_list() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(ArrayList.class.getName());
        int n = 0;
        for(Instance i : jclass.getInstances()) {
            int m = 0;
            for(Instance e :  HeapWalker.walk(i, "elementData[*](**.DummyA)")) {
                ++m;
            }
            if (m != 0) {
                ++n;
                assertThat(m).isEqualTo(50);
            }
        }

        assertThat(n).isEqualTo(1);
    }

    @Test
    public void verify_DummyC_field_access() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyC.class.getName());

        assertThat(jclass.getInstances()).hasSize(1);

        Instance i = jclass.getInstances().get(0);

        assertThat(HeapWalker.valueOf(i, "structField.trueField")).isEqualTo(true);
        assertThat(HeapWalker.valueOf(i, "structField.falseField")).isEqualTo(false);
        assertThat(HeapWalker.valueOf(i, "structField.byteField")).isEqualTo((byte) 13);
        assertThat(HeapWalker.valueOf(i, "structField.shortField")).isEqualTo((short) -14);
        assertThat(HeapWalker.valueOf(i, "structField.charField")).isEqualTo((char) 15);
        assertThat(HeapWalker.valueOf(i, "structField.intField")).isEqualTo(0x66666666);
        assertThat(HeapWalker.valueOf(i, "structField.longField")).isEqualTo(0x6666666666l);
        assertThat(HeapWalker.valueOf(i, "structField.floatField")).isEqualTo(0.1f);
        assertThat(HeapWalker.valueOf(i, "structField.doubleField")).isEqualTo(-0.2);

        assertThat(HeapWalker.valueOf(i, "structField.trueBoxedField")).isEqualTo(true);
        assertThat(HeapWalker.valueOf(i, "structField.falseBoxedField")).isEqualTo(false);
        assertThat(HeapWalker.valueOf(i, "structField.byteBoxedField")).isEqualTo((byte) 13);
        assertThat(HeapWalker.valueOf(i, "structField.shortBoxedField")).isEqualTo((short) -14);
        assertThat(HeapWalker.valueOf(i, "structField.charBoxedField")).isEqualTo((char) 15);
        assertThat(HeapWalker.valueOf(i, "structField.intBoxedField")).isEqualTo(0x66666666);
        assertThat(HeapWalker.valueOf(i, "structField.longBoxedField")).isEqualTo(0x6666666666l);
        assertThat(HeapWalker.valueOf(i, "structField.floatBoxedField")).isEqualTo(0.1f);
        assertThat(HeapWalker.valueOf(i, "structField.doubleBoxedField")).isEqualTo(-0.2);

        assertThat(HeapWalker.valueOf(i, "structField.textField")).isEqualTo("this is struct");

        assertThat(HeapWalker.valueOf(i, "structArray[*].trueField")).isEqualTo(true);
        assertThat(HeapWalker.valueOf(i, "structArray[*].falseField")).isEqualTo(false);
        assertThat(HeapWalker.valueOf(i, "structArray[*].byteField")).isEqualTo((byte) 13);
        assertThat(HeapWalker.valueOf(i, "structArray[*].shortField")).isEqualTo((short) -14);
        assertThat(HeapWalker.valueOf(i, "structArray[*].charField")).isEqualTo((char) 15);
        assertThat(HeapWalker.valueOf(i, "structArray[*].intField")).isEqualTo(0x66666666);
        assertThat(HeapWalker.valueOf(i, "structArray[*].longField")).isEqualTo(0x6666666666l);
        assertThat(HeapWalker.valueOf(i, "structArray[*].floatField")).isEqualTo(0.1f);
        assertThat(HeapWalker.valueOf(i, "structArray[*].doubleField")).isEqualTo(-0.2);

        assertThat(HeapWalker.valueOf(i, "structArray[*].trueBoxedField")).isEqualTo(true);
        assertThat(HeapWalker.valueOf(i, "structArray[*].falseBoxedField")).isEqualTo(false);
        assertThat(HeapWalker.valueOf(i, "structArray[*].byteBoxedField")).isEqualTo((byte) 13);
        assertThat(HeapWalker.valueOf(i, "structArray[*].shortBoxedField")).isEqualTo((short) -14);
        assertThat(HeapWalker.valueOf(i, "structArray[*].charBoxedField")).isEqualTo((char) 15);
        assertThat(HeapWalker.valueOf(i, "structArray[*].intBoxedField")).isEqualTo(0x66666666);
        assertThat(HeapWalker.valueOf(i, "structArray[*].longBoxedField")).isEqualTo(0x6666666666l);
        assertThat(HeapWalker.valueOf(i, "structArray[*].floatBoxedField")).isEqualTo(0.1f);
        assertThat(HeapWalker.valueOf(i, "structArray[*].doubleBoxedField")).isEqualTo(-0.2);

        assertThat(HeapWalker.valueOf(i, "structArray[*].textField")).isEqualTo("this is struct #1");
    }

    private Set<String> testSet(String pref, int limit) {
        Set<String> result = new TreeSet<String>();
        for(int i = 0; i != limit; ++i) {
            result.add(pref + i);
        }
        return result;
    }
}
