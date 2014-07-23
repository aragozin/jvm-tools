package org.gridkit.jvmtool.util;

import java.util.Arrays;

class PagedLongArray {

	private final static int PAGE_BITS = 10;
	private final static int PAGE_MASK = ~(-1 << PAGE_BITS);
	private final static int PAGE_SIZE = 1 << PAGE_BITS;

	public final static long NULL_VALUE = 0;

	protected long lastIndex = -1;
	protected long[][] array = new long[16][];


    public long get(long n) {
		int bi = (int) (n >> PAGE_BITS);
		if (bi >= array.length) {
			return NULL_VALUE;
		}
		if (bi < 0) {
		    throw new ArrayIndexOutOfBoundsException(bi);
		}
		long[] page = array[bi];
		if (page == null) {
			return NULL_VALUE;
		}
		return page[(int) (n & PAGE_MASK)];
	}

    public long seekNext(long start) {
        long n = start;
        while(true) {
            int bi = (int) (n >> PAGE_BITS);
            if (bi >= array.length) {
                return -1;
            }
            if (bi < 0) {
                throw new ArrayIndexOutOfBoundsException(bi);
            }
            long[] page = array[bi];
            if (page == null) {
                n = PAGE_SIZE * (bi + 1);
                continue;
            }
            if (page[(int) (n & PAGE_MASK)] != 0) {
                return n;
            }
            ++n;
        }
    }

    public void set(long n, long value) {
		lastIndex = Math.max(lastIndex, n);
		int bi = (int) (n >> PAGE_BITS);
		if (bi >= array.length) {
			array = Arrays.copyOf(array, bi + 1);
		}
		long[] page = array[bi];
		if (page == null) {
		    if (value == NULL_VALUE) {
		        return;
		    }
			array[bi] = page = new long[PAGE_SIZE];
			Arrays.fill(page, NULL_VALUE);
		}
		page[(int) (n & PAGE_MASK)] = value;
	}
}
