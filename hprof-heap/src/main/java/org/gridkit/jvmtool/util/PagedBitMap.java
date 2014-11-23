package org.gridkit.jvmtool.util;
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


import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple bit map using paged long array for storage.
 * Untouched pages are not allocated, so it is reasonably efficient
 * for bitmaps with large gaps.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class PagedBitMap {

    private LongArray array;
    
    public PagedBitMap() {
        this(false);
    }

    public PagedBitMap(boolean sparse) {
        if (sparse) {
            array = new SparsePagedLongArray();
        }
        else {
            array = new PagedLongArray();
        }
    }

    public boolean get(long index) {
        long lindex = index / 64;
        long bit = 1l << (index % 64);
        return (0 != (bit & array.get(lindex))); // Long.toBinaryString(array.get(lindex))
    }

    public long seekOne(long start) {
        long n = start;
        while(true) {
            long lindex = n / 64;
            long bit = 1l << (n % 64);

            long word = array.get(lindex);
            if (word == 0) {
                long nn = array.seekNext(lindex);
                if (nn < 0) {
                    return -1;
                }
                n = 64 * nn;
                continue;
            }
            while(bit != 0) {
                if (0 != (bit & word)) {
                    return n;
                }
                ++n;
                bit <<= 1;
            }
        }
    }

    public void set(long index, boolean value) {
        long lindex = index / 64;
        long bit = 1l << (index % 64);
        if (value) {
            array.set(lindex, bit | array.get(lindex));
        }
        else {
            array.set(lindex, (~bit) & array.get(lindex));
        }
    }

    public boolean getAndSet(long index, boolean value) {
        long lindex = index / 64;
        long bit = 1l << (index % 64);
        long ov = array.get(lindex);
        if (value) {
        array.set(lindex, bit | ov);
        }
        else {
            array.set(lindex, (~bit) & ov);
        }
        return 0 != (bit & ov);
    }

    /**
     * Bitwise <br/>
     * <code>this = this | that</code>
     */
    public void add(PagedBitMap that) {
        LongArray ta = that.array;
        long n = 0;
        while(true) {
            n = ta.seekNext(n);
            if (n < 0) {
                break;
            }
            long v = array.get(n) | ta.get(n);
            array.set(n, v);
            ++n;
        }
    }

    /**
     * Bitwise <br/>
     * <code>overflow = this & that</code>
     * <br/>
     * <code>this = this | that</code>
     */
    public void addWithOverflow(PagedBitMap that, PagedBitMap overflow) {
        LongArray ta = that.array;
        LongArray of = overflow.array;
        long n = 0;
        while(true) {
            n = ta.seekNext(n);
            if (n < 0) {
                break;
            }
            long o = array.get(n) & ta.get(n);
            long v = array.get(n) | ta.get(n);
            array.set(n, v);
            if (o != 0) {
                o |= of.get(n);
                of.set(n, o);
            }
            ++n;
        }
    }

    /**
     * Bitwise <br/>
     * <code>this = this & (~that)</code>
     */
    public void sub(PagedBitMap that) {
        LongArray ta = that.array;
        long n = 0;
        while(true) {
            n = ta.seekNext(n);
            if (n < 0) {
                break;
            }
            long v = array.get(n) & ~ta.get(n);
            array.set(n, v);
            ++n;
        }
    }

    /**
     * Bitwise <br/>
     * <code>this = this & that</code>
     */
    public void mult(PagedBitMap that) {
        LongArray ta = that.array;
        long n = 0;
        while(true) {
            n = ta.seekNext(n);
            if (n < 0) {
                break;
            }
            long v = array.get(n) & ta.get(n);
            array.set(n, v);
            ++n;
        }
    }

    public Iterable<Long> ones() {
        return new Iterable<Long>() {
            @Override
            public Iterator<Long> iterator() {
                return new SeekerIterator(PagedBitMap.this);
            }
        };
    }
    
    @SuppressWarnings("unused")
    public long countOnes() {
        long n = 0;
        for(Long l: ones()) {
            ++n;
        }
        return n;
    }

    protected static class SeekerIterator implements Iterator<Long> {

        private PagedBitMap bitmap;
        private long next;

        public SeekerIterator(PagedBitMap bitmap) {
            this.bitmap = bitmap;
            this.next = bitmap.seekOne(0);
        }

        @Override
        public boolean hasNext() {
            return next != -1;
        }

        @Override
        public Long next() {
            if (next == -1) {
                throw new NoSuchElementException();
            }
            long n = next;
            next = bitmap.seekOne(next + 1);
            return n;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }    
}
