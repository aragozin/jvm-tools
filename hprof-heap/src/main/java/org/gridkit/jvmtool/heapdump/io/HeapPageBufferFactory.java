package org.gridkit.jvmtool.heapdump.io;

public class HeapPageBufferFactory implements PagePoolFactory {
    
    private final long memoryLimit;
    private final int allocPageSize;
    
    public HeapPageBufferFactory(int allocPageSize, long memoryLimit) {
        if (Integer.bitCount(allocPageSize) != 1) {
            throw new IllegalArgumentException("allocPageSize " + allocPageSize + " is invalid, should be power of two");
        }
        this.memoryLimit = memoryLimit;
        this.allocPageSize = allocPageSize;
    }

    @Override
    public PagePool createPagePool(int pageSize) {
        if (Integer.bitCount(allocPageSize) != 1) {
            throw new IllegalArgumentException("allocPageSize " + allocPageSize + " is invalid, should be power of two");
        }
        return new ByteBufferPageManager(pageSize, Math.max(pageSize, allocPageSize), memoryLimit);
    }
}
