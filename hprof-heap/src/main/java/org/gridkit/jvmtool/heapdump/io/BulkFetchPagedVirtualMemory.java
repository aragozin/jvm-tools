package org.gridkit.jvmtool.heapdump.io;

import java.nio.ByteBuffer;

public abstract class BulkFetchPagedVirtualMemory extends PagedVirtualMemory {

    private final int fetchFactor;
    
    public BulkFetchPagedVirtualMemory(PagePool pagePool, int fetchFactor) {
        super(pagePool);
        this.fetchFactor = fetchFactor;
    }

    protected abstract int readPage(long offset, ByteBuffer page);

    @Override
    protected void loadPage(int pageId) {
        
        int fp = 0;
        for(int i = 0; i != fetchFactor; ++i) {
            if (!isPageMapped(pageId + fp)) {
                ++fp;
            }
        }
        
        PageInfo[] p = new PageInfo[fp];
        reclaimPages(p);

        int pn = 0;
        for(int i = 0; i != fetchFactor && pn < p.length; ++i) {
            int lp = pageId + i;
            if (isPageMapped(lp)) {
                continue;
            }
            PageInfo pi = p[pn++];
            long offs = ((long)lp) * pageSize;
            pi.buffer.clear();
            int n = readPage(offs, pi.buffer);
            pi.buffer.position(0);
            pi.buffer.limit(n);
            
            if (n > 0) {
                mapPage(lp, pi);
            }
        }        
    }
}
