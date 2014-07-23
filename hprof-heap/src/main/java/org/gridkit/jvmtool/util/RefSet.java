package org.gridkit.jvmtool.util;



public class RefSet extends PagedBitMap {

    @Override
    public boolean get(long index) {
        if (index % 8 != 0) {
            throw new IllegalArgumentException("" + index);
        }
        return super.get(index / 8);
    }

    @Override
    public long seekNext(long index) {
        index = (index + 7) / 8; // alligning to 8 byte boundary
        return 8 * super.seekNext(index);
    }

    @Override
    public boolean getAndSet(long index, boolean value) {
        if (index % 8 != 0) {
            throw new IllegalArgumentException("" + index);
        }
        return super.getAndSet(index / 8, value);
    }

    @Override
    public void set(long index, boolean value) {
        if (index % 8 != 0) {
            throw new IllegalArgumentException("" + index);
        }
        super.set(index / 8, value);
    }

    @Override
    public void sub(PagedBitMap that) {
        super.sub(that);
    }
}
