package org.gridkit.jvmtool.util;

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

    private PagedLongArray array = new PagedLongArray();

    public boolean get(long index) {
        long lindex = index / 64;
        long bit = 1l << (index % 64);
        return (0 != (bit & array.get(lindex))); // Long.toBinaryString(array.get(lindex))
    }

    public long seekNext(long start) {
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

    public void add(PagedBitMap that) {
        PagedLongArray ta = that.array;
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

    public void sub(PagedBitMap that) {
        PagedLongArray ta = that.array;
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

    public void mult(PagedBitMap that) {
        PagedLongArray ta = that.array;
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

    protected static class SeekerIterator implements Iterator<Long> {

        private PagedBitMap bitmap;
        private long next;

        public SeekerIterator(PagedBitMap bitmap) {
            this.bitmap = bitmap;
            this.next = bitmap.seekNext(0);
        }

        @Override
        public boolean hasNext() {
            return next >= 0;
        }

        @Override
        public Long next() {
            if (next < 0) {
                throw new NoSuchElementException();
            }
            long n = next;
            next = bitmap.seekNext(next + 1);
            return n;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
