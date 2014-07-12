/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.heap;

import java.util.Arrays;
import java.util.Map;

/**
 * Map for longs. IdentityHashMap was used as template.
 * Zero cannot be used as key. Load factor is 3/4.
 * @author Tomas Hurka
 */
class LongHashMap {
    /**
     * The initial capacity used by the no-args constructor.
     * MUST be a power of two.
     */
    private static final int DEFAULT_CAPACITY = 32;

    /**
     * The minimum capacity, used if a lower value is implicitly specified
     * by either of the constructors with arguments.  The value 4 corresponds
     * to an expected maximum size of 2, given a load factor of 2/3.
     * MUST be a power of two.
     */
    private static final int MINIMUM_CAPACITY = 4;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<29.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 29;

    /**
     * The table, resized as necessary. Length MUST always be a power of two.
     */
    private transient long[] table;

    /**
     * The number of key-value mappings contained in this identity hash map.
     *
     * @serial
     */
    private int size;

    /**
     * The number of modifications, to support fast-fail iterators
     */
    private transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private transient int threshold;

    /**
     * Constructs a new, empty identity hash map with a default expected
     * maximum size.
     */
    LongHashMap() {
        init(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a new, empty map with the specified expected maximum size.
     * Putting more than the expected number of key-value mappings into
     * the map may cause the internal data structure to grow, which may be
     * somewhat time-consuming.
     *
     * @param expectedMaxSize the expected maximum size of the map
     * @throws IllegalArgumentException if <tt>expectedMaxSize</tt> is negative
     */
    LongHashMap(int expectedMaxSize) {
        if (expectedMaxSize < 0)
            throw new IllegalArgumentException("expectedMaxSize is negative: "
                                               + expectedMaxSize);
        init(capacity(expectedMaxSize));
    }

    /**
     * Returns the appropriate capacity for the specified expected maximum
     * size.  Returns the smallest power of two between MINIMUM_CAPACITY
     * and MAXIMUM_CAPACITY, inclusive, that is greater than
     * (4 * expectedMaxSize)/3, if such a number exists.  Otherwise
     * returns MAXIMUM_CAPACITY.  If (4 * expectedMaxSize)/3 is negative, it
     * is assumed that overflow has occurred, and MAXIMUM_CAPACITY is returned.
     */
    private int capacity(int expectedMaxSize) {
        // Compute min capacity for expectedMaxSize given a load factor of 3/4
        int minCapacity = (4 * expectedMaxSize)/3;

        // Compute the appropriate capacity
        int result;
        if (minCapacity > MAXIMUM_CAPACITY || minCapacity < 0) {
            result = MAXIMUM_CAPACITY;
        } else {
            result = MINIMUM_CAPACITY;
            while (result < minCapacity)
                result <<= 1;
        }
        return result;
    }

    /**
     * Initializes object to be an empty map with the specified initial
     * capacity, which is assumed to be a power of two between
     * MINIMUM_CAPACITY and MAXIMUM_CAPACITY inclusive.
     */
    private void init(int initCapacity) {
        assert (initCapacity & -initCapacity) == initCapacity; // power of 2
        assert initCapacity >= MINIMUM_CAPACITY;
        assert initCapacity <= MAXIMUM_CAPACITY;

        threshold = (initCapacity * 3)/ 4;
        table = new long[2 * initCapacity];
    }

    /**
     * Returns the number of key-value mappings in this identity hash map.
     *
     * @return the number of key-value mappings in this map
     */
    int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this identity hash map contains no key-value
     * mappings.
     *
     * @return <tt>true</tt> if this identity hash map contains no key-value
     *         mappings
     */
    boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns index for Object x.
     */
    private static int hash(long x, int length) {
        int h = (int)(x ^ (x >>> 32));
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        h ^= (h >>> 7) ^ (h >>> 4);
        return (h) & (length - 2);
    }

    /**
     * Circularly traverses table of size len.
     */
    private static int nextKeyIndex(int i, int len) {
        return (i + 2 < len ? i + 2 : 0);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key == k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    long get(long key) {
        long k = key;
        long[] tab = table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            long item = tab[i];
            if (item == k)
                return tab[i + 1];
            if (item == 0)
                return 0;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Tests whether the specified object reference is a key in this identity
     * hash map.
     *
     * @param   key   possible key
     * @return  <code>true</code> if the specified object reference is a key
     *          in this map
     * @see     #containsValue(Object)
     */
    boolean containsKey(long key) {
        long k = key;
        long[] tab = table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            long item = tab[i];
            if (item == k)
                return true;
            if (item == 0)
                return false;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Tests whether the specified object reference is a value in this identity
     * hash map.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified object reference
     * @see     #containsKey(Object)
     */
    boolean containsValue(long value) {
        long[] tab = table;
        for (int i = 1; i < tab.length; i += 2)
            if (tab[i] == value && tab[i - 1] != 0)
                return true;

        return false;
    }

    /**
     * Tests if the specified key-value mapping is in the map.
     *
     * @param   key   possible key
     * @param   value possible value
     * @return  <code>true</code> if and only if the specified key-value
     *          mapping is in the map
     */
    private boolean containsMapping(long key, long value) {
        long k = key;
        long[] tab = table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            long item = tab[i];
            if (item == k)
                return tab[i + 1] == value;
            if (item == 0)
                return false;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Associates the specified value with the specified key in this identity
     * hash map.  If the map previously contained a mapping for the key, the
     * old value is replaced.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     * @see     Object#equals(Object)
     * @see     #get(Object)
     * @see     #containsKey(Object)
     */
    long put(long key, long value) {
        assert key != 0;
        long k = key;
        long[] tab = table;
        int len = tab.length;
        int i = hash(k, len);

        long item;
        while ( (item = tab[i]) != 0) {
            if (item == k) {
                long oldValue = tab[i + 1];
                tab[i + 1] = value;
                return oldValue;
            }
            i = nextKeyIndex(i, len);
        }

        modCount++;
        tab[i] = k;
        tab[i + 1] = value;
        if (++size >= threshold)
            resize(len); // len == 2 * current capacity.
        return 0;
    }

    /**
     * Resize the table to hold given capacity.
     *
     * @param newCapacity the new capacity, must be a power of two.
     */
    private void resize(int newCapacity) {
        // assert (newCapacity & -newCapacity) == newCapacity; // power of 2
        int newLength = newCapacity * 2;

        long[] oldTable = table;
        int oldLength = oldTable.length;
        if (oldLength == 2*MAXIMUM_CAPACITY) { // can't expand any further
            if (threshold == MAXIMUM_CAPACITY-1)
                throw new IllegalStateException("Capacity exhausted.");
            threshold = MAXIMUM_CAPACITY-1;  // Gigantic map!
            return;
        }
        if (oldLength >= newLength)
            return;

        long[] newTable = new long[newLength];
        threshold = (newCapacity * 3) / 4;

        for (int j = 0; j < oldLength; j += 2) {
            long key = oldTable[j];
            if (key != 0) {
                long value = oldTable[j+1];
                int i = hash(key, newLength);
                while (newTable[i] != 0)
                    i = nextKeyIndex(i, newLength);
                newTable[i] = key;
                newTable[i + 1] = value;
            }
        }
        table = newTable;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    void putAll(Map<Long,Long> m) {
        int n = m.size();
        if (n == 0)
            return;
        if (n > threshold) // conservatively pre-expand
            resize(capacity(n));

        for (Map.Entry<Long,Long> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    long remove(long key) {
        long k = key;
        long[] tab = table;
        int len = tab.length;
        int i = hash(k, len);

        while (true) {
            long item = tab[i];
            if (item == k) {
                modCount++;
                size--;
                long oldValue = tab[i + 1];
                tab[i + 1] = 0;
                tab[i] = 0;
                closeDeletion(i);
                return oldValue;
            }
            if (item == 0)
                return -1;
            i = nextKeyIndex(i, len);
        }

    }

    /**
     * Removes the specified key-value mapping from the map if it is present.
     *
     * @param   key   possible key
     * @param   value possible value
     * @return  <code>true</code> if and only if the specified key-value
     *          mapping was in the map
     */
    private boolean removeMapping(long key, long value) {
        long k = key;
        long[] tab = table;
        int len = tab.length;
        int i = hash(k, len);

        while (true) {
            long item = tab[i];
            if (item == k) {
                if (tab[i + 1] != value)
                    return false;
                modCount++;
                size--;
                tab[i] = 0;
                tab[i + 1] = 0;
                closeDeletion(i);
                return true;
            }
            if (item == 0)
                return false;
            i = nextKeyIndex(i, len);
        }
    }

    /**
     * Rehash all possibly-colliding entries following a
     * deletion. This preserves the linear-probe
     * collision properties required by get, put, etc.
     *
     * @param d the index of a newly empty deleted slot
     */
    private void closeDeletion(int d) {
        // Adapted from Knuth Section 6.4 Algorithm R
        long[] tab = table;
        int len = tab.length;

        // Look for items to swap into newly vacated slot
        // starting at index immediately following deletion,
        // and continuing until a null slot is seen, indicating
        // the end of a run of possibly-colliding keys.
        long item;
        for (int i = nextKeyIndex(d, len); (item = tab[i]) != 0;
             i = nextKeyIndex(i, len) ) {
            // The following test triggers if the item at slot i (which
            // hashes to be at slot r) should take the spot vacated by d.
            // If so, we swap it in, and then continue with d now at the
            // newly vacated i.  This process will terminate when we hit
            // the null slot at the end of this run.
            // The test is messy because we are using a circular table.
            int r = hash(item, len);
            if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
                tab[d] = item;
                tab[d + 1] = tab[i + 1];
                tab[i] = 0;
                tab[i + 1] = 0;
                d = i;
            }
        }
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    void clear() {
        modCount++;
        long[] tab = table;
        Arrays.fill(tab, 0);
        size = 0;
    }

    /**
     * Compares the specified object with this map for equality.  Returns
     * <tt>true</tt> if the given object is also a map and the two maps
     * represent identical object-reference mappings.  More formally, this
     * map is equal to another map <tt>m</tt> if and only if
     * <tt>this.entrySet().equals(m.entrySet())</tt>.
     *
     * <p><b>Owing to the reference-equality-based semantics of this map it is
     * possible that the symmetry and transitivity requirements of the
     * <tt>Object.equals</tt> contract may be violated if this map is compared
     * to a normal map.  However, the <tt>Object.equals</tt> contract is
     * guaranteed to hold among <tt>LongHashMap</tt> instances.</b>
     *
     * @param  o object to be compared for equality with this map
     * @return <tt>true</tt> if the specified object is equal to this map
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof LongHashMap) {
            LongHashMap m = (LongHashMap) o;
            if (m.size() != size)
                return false;

            long[] tab = m.table;
            for (int i = 0; i < tab.length; i+=2) {
                long k = tab[i];
                if (k != 0 && !containsMapping(k, tab[i + 1]))
                    return false;
            }
            return true;
        } else if (o instanceof Map) {
            Map m = (Map)o;
            return false;
        } else {
            return false;  // o is not a Map
        }
    }

    /**
     * Returns the hash code value for this map.  The hash code of a map is
     * defined to be the sum of the hash codes of each entry in the map's
     * <tt>entrySet()</tt> view.  This ensures that <tt>m1.equals(m2)</tt>
     * implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two
     * <tt>IdentityHashMap</tt> instances <tt>m1</tt> and <tt>m2</tt>, as
     * required by the general contract of {@link Object#hashCode}.
     *
     * <p><b>Owing to the reference-equality-based semantics of the
     * <tt>Map.Entry</tt> instances in the set returned by this map's
     * <tt>entrySet</tt> method, it is possible that the contractual
     * requirement of <tt>Object.hashCode</tt> mentioned in the previous
     * paragraph will be violated if one of the two objects being compared is
     * an <tt>LongHashMap</tt> instance and the other is a normal map.</b>
     *
     * @return the hash code value for this map
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        int result = 0;
        long[] tab = table;
        for (int i = 0; i < tab.length; i +=2) {
            long key = tab[i];
            if (key != 0) {
                long k = key;
                result += hash(k,tab.length) ^
                          hash(tab[i + 1],tab.length);
            }
        }
        return result;
    }


}
