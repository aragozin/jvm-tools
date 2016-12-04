package org.gridkit.jvmtool.heapdump.io;

import java.nio.ByteBuffer;

public abstract class SimplePagedVirtualMemory extends PagedVirtualMemory {

    
    public SimplePagedVirtualMemory(PagePool pagePool) {
        super(pagePool);
    }

    protected abstract int readPage(long offset, ByteBuffer page);

    @Override
    protected void loadPage(int pageId) {
        PageInfo[] p = new PageInfo[1];
        reclaimPages(p);
        
        PageInfo pi = p[0];
        long offs = ((long)pageId) * pageSize;
        pi.buffer.clear();
        int n = readPage(offs, pi.buffer);
        pi.buffer.position(0);
        pi.buffer.limit(n);
        
        mapPage(pageId, pi);
    }
}
