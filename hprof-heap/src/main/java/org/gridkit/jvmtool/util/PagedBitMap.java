package org.gridkit.jvmtool.util;



class PagedBitMap {

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
        array.set(lindex, bit | array.get(lindex));
    }

    public boolean getAndSet(long index, boolean value) {
        long lindex = index / 64;
        long bit = 1l << (index % 64);
        long ov = array.get(lindex);
        array.set(lindex, bit | ov);
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
}
