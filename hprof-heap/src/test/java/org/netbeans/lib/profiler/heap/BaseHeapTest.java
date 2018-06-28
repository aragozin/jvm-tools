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
import static org.gridkit.jvmtool.heapdump.HeapWalker.valueOf;
import static org.gridkit.jvmtool.heapdump.HeapWalker.walkFirst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.assertj.core.api.Assertions;
import org.gridkit.jvmtool.heapdump.HeapPathHelper;
import org.gridkit.jvmtool.heapdump.HeapWalker;
import org.gridkit.jvmtool.heapdump.InboundAnalyzer;
import org.junit.Assert;
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
    public void verify_inbound_ref_reporter() {

        Heap heap = getHeap();
        
        InboundAnalyzer ia = new InboundAnalyzer(heap);
        ia.initRoots();
        
        Instance i = heap.getJavaClassByName(DummyB.class.getName()).getInstances().get(0);
        ia.mark(i.getInstanceId());
        
        int d = 1;
        while(!ia.isExhausted()) {
        	System.out.println("\nDepth " + d);
        	ia.report();
        	++d;
        }
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
    public void verify_heap_path_for_arrays() {

        boolean[] bool_values = {true, false};
        byte[] byte_values = {Byte.MIN_VALUE, Byte.MAX_VALUE};
        short[] short_values = {Short.MIN_VALUE, Short.MAX_VALUE};
        char[] char_values = {Character.MIN_VALUE, Character.MAX_VALUE};
        int[] int_values = {Integer.MIN_VALUE, Integer.MAX_VALUE};
        long[] long_values = {Long.MIN_VALUE, Long.MAX_VALUE};
        float[] float_values = {Float.MIN_VALUE, Float.NaN, Float.MAX_VALUE};
        double[] double_values = {Double.MIN_VALUE, Double.NaN, Double.MAX_VALUE};
        
        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyC.class.getName());
        int n = 0;

        for(Instance i : jclass.getInstances()) {
            assertArrayEquals(bool_values, valueOf(i, "bool_values"));
            assertArrayEquals(byte_values, valueOf(i, "byte_values"));
            assertArrayEquals(short_values, valueOf(i, "short_values"));
            assertArrayEquals(char_values, valueOf(i, "char_values"));
            assertArrayEquals(int_values, valueOf(i, "int_values"));
            assertArrayEquals(long_values, valueOf(i, "long_values"));
            assertArrayEquals(float_values, valueOf(i, "float_values"));
            assertArrayEquals(double_values, valueOf(i, "double_values"));

            assertThat(valueOf(i, "bool_values[0]")).isEqualTo(Boolean.TRUE);
            assertThat(valueOf(i, "byte_values[0]")).isEqualTo(Byte.MIN_VALUE);
            assertThat(valueOf(i, "short_values[0]")).isEqualTo(Short.MIN_VALUE);
            assertThat(valueOf(i, "char_values[0]")).isEqualTo(Character.MIN_VALUE);
            assertThat(valueOf(i, "int_values[0]")).isEqualTo(Integer.MIN_VALUE);
            assertThat(valueOf(i, "long_values[0]")).isEqualTo(Long.MIN_VALUE);
            assertThat(valueOf(i, "float_values[0]")).isEqualTo(Float.MIN_VALUE);
            assertThat(valueOf(i, "double_values[0]")).isEqualTo(Double.MIN_VALUE);
            
            ++n;
        }

        assertThat(n).isEqualTo(1);
    }
    
    @Test
    public void verify_heap_path_tracker_over_null() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyD.class.getName());
        Instance i = jclass.getInstances().get(0);
        Assert.assertNull(HeapPathHelper.trackFirst(i, "nested.value"));
        Assert.assertNull(HeapPathHelper.trackFirst(i, "nestedArray[0].value"));
        Assert.assertEquals(".nestedArray[1].value", HeapPathHelper.trackFirst(i, "nestedArray[*].value"));
    }

    @Test
    public void verify_heap_path_walker_over_null() {
        
        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyD.class.getName());
        Instance i = jclass.getInstances().get(0);
        Assert.assertNull(HeapWalker.walkFirst(i, "nested.value"));
        Assert.assertNull(HeapWalker.walkFirst(i, "nestedArray[0].value"));
        Assert.assertEquals("somevalue", HeapWalker.valueOf(HeapWalker.walkFirst(i, "nestedArray[*].value")));
    }

    @Test
    public void verify_heap_path_walker_inverted_predicate() {
    	
    	Heap heap = getHeap();
    	
    	assertSetsAreEqual(a("A", "B", "C", "D", "E"), scan(heap.getAllInstances(), "(**DummyP)key"));
    	assertSetsAreEqual(a("A", "C", "E"), scan(heap.getAllInstances(), "(**DummyP)[value!=null]key"));
    	assertSetsAreEqual(a("B", "D"), scan(heap.getAllInstances(), "(**DummyP)[value=null]key"));
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

    @Test
    public void verify_asterisk_paths() {

        Heap heap = getHeap();
        JavaClass jclass = heap.getJavaClassByName(DummyN.MyInnerClass.class.getName());

        List<Instance> set = jclass.getInstances();
		assertThat(set).hasSize(5);

    	
    	assertSetsAreEqual(a("dummyA", "dummyB"), scan(set, "*(**DummyN).dummyName"));
    	assertSetsAreEqual(a("dummyA", "dummyB"), scan(set, "*(**DummyN).*"));
    	assertSetsAreEqual(a("dummyA", "dummyB"), scan(set, "*.dummyName"));
    	assertSetsAreEqual(a("dummyA"), scan(set, "[innerName=A.1]*(**DummyN).dummyName"));
    	assertSetsAreEqual(a("dummyB"), scan(set, "[innerName=B.1]*(**DummyN).dummyName"));
    	assertSetsAreEqual(a("dummyB"), scan(set, "[innerName=B.1]*(**DummyN).*"));
    	assertSetsAreEqual(a("dummyA"), scan(set, "[innerName=A.4]*.dummyName"));
    }

    @Test
    public void verify_single_asterisk_path() {
    	
    	Heap heap = getHeap();
    	JavaClass jclass = heap.getJavaClassByName(DummyA.class.getName());
    	
    	List<Instance> set = jclass.getInstances();
    	
    	
    	assertThat(scan(set, "*")).hasSize(50);
    }
    
    @Test
    public void verify_string_decoding_latin() {
    	Heap heap = getHeap();
    	JavaClass jclass = heap.getJavaClassByName(DummyS.class.getName());
    	
    	DummyS proto = new DummyS();
    	Instance dummyS = jclass.getInstances().get(0);
    	
    	assertThat(valueOf(dummyS, "latinString")).isEqualTo(proto.latinString);    	
    }

    @Test
    public void verify_string_decoding_cyrillic() {
    	Heap heap = getHeap();
    	JavaClass jclass = heap.getJavaClassByName(DummyS.class.getName());
    	
    	DummyS proto = new DummyS();
    	Instance dummyS = jclass.getInstances().get(0);
    	
    	assertThat(valueOf(dummyS, "cyrillicString")).isEqualTo(proto.cyrillicString);
    }

    @Test
    public void verify_string_decoding_unicode() {
    	Heap heap = getHeap();
    	JavaClass jclass = heap.getJavaClassByName(DummyS.class.getName());
    	
    	DummyS proto = new DummyS();
    	Instance dummyS = jclass.getInstances().get(0);
    	
    	assertThat(valueOf(dummyS, "unicodeString")).isEqualTo(proto.unicodeString);
    }
    
    private Object[] scan(Iterable<Instance> instances, String path) {
    	List<Object> val = new ArrayList<Object>();
    	for(Instance i : instances) {
    		Object v = HeapWalker.valueOf(i, path);
    		if (v != null) {
    			val.add(v);
    		}
    	}
    	return val.toArray(new Object[0]);
    }
    
    private static Object[] a(Object... a) {
    	return a;
    }
    
    private static void assertSetsAreEqual(Object[] expected, Object... actual) {
    	Set<Object> e = new HashSet<Object>(Arrays.asList(expected));
    	Set<Object> a = new HashSet<Object>(Arrays.asList(actual));
    	
    	StringBuilder miss = new StringBuilder();
    	StringBuilder extra = new StringBuilder();
    	
    	for(Object x: e) {
    		if (a.contains(x)) {
    			a.remove(x);
    		}
    		else {
    			if (miss.length() > 0) {
    				miss.append(", ");
    			}
    			miss.append(x);
    		}
    	}
    	for(Object y: a) {
			if (extra.length() > 0) {
				extra.append(", ");
			}
			extra.append(y);    		
    	}
    	
    	if (miss.length() > 0 || extra.length() > 0) {
    		// for better message is IDE
    		Assert.assertEquals("", "Missing: " + miss + " Unexpected: " + extra);;
    	}
    }
    
    private static void assertArrayEquals(Object expected, Object actual) {
        if (expected instanceof boolean[]) {
            assertThat(Arrays.toString((boolean[])actual)).isEqualTo(Arrays.toString((boolean[])expected));
        }
        else 
        if (expected instanceof byte[]) {
            assertThat(Arrays.toString((byte[])actual)).isEqualTo(Arrays.toString((byte[])expected));
        }
        else 
        if (expected instanceof short[]) {
            assertThat(Arrays.toString((short[])actual)).isEqualTo(Arrays.toString((short[])expected));
        }
        else 
        if (expected instanceof char[]) {
            assertThat(Arrays.toString((char[])actual)).isEqualTo(Arrays.toString((char[])expected));
        }
        else 
        if (expected instanceof int[]) {
            assertThat(Arrays.toString((int[])actual)).isEqualTo(Arrays.toString((int[])expected));
        }
        else 
        if (expected instanceof long[]) {
            assertThat(Arrays.toString((long[])actual)).isEqualTo(Arrays.toString((long[])expected));
        }
        else 
        if (expected instanceof float[]) {
            assertThat(Arrays.toString((float[])actual)).isEqualTo(Arrays.toString((float[])expected));
        }
        else 
        if (expected instanceof double[]) {
            assertThat(Arrays.toString((double[])actual)).isEqualTo(Arrays.toString((double[])expected));
        }
        else { 
            Assertions.fail("Array type expected, but was " + actual);
        }
    }

    private Set<String> testSet(String pref, int limit) {
        Set<String> result = new TreeSet<String>();
        for(int i = 0; i != limit; ++i) {
            result.add(pref + i);
        }
        return result;
    }
}
