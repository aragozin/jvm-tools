package org.gridkit.jvmtool.heapdump.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ByteBufferPageManager implements PagePool {

    private List<ByteBuffer> resourcePages = new ArrayList<ByteBuffer>();
    private Map<ByteBuffer, BW> allPages = new IdentityHashMap<ByteBuffer, BW>();
    private Set<BW> freePages = new LinkedHashSet<BW>();
    
    private final long memoryLimit;
    private long memoryUsed;
    private boolean atLimit;

    protected final int resourcePageSize;
    private final int pageSize;
    
    public ByteBufferPageManager(int pageSize, long limit) {
        this(pageSize, pageSize, limit);
    }

    public ByteBufferPageManager(int pageSize, int allocSize, long limit) {
        this.memoryLimit = limit;
        this.resourcePageSize = allocSize;
        this.pageSize = pageSize;
        
        if (pageSize * (resourcePageSize / pageSize) != resourcePageSize) {
            throw new IllegalArgumentException("Alloc size should be divisible by page size");
        }
    }
    
    protected ByteBuffer allocatePage() {
        try {
            return ByteBuffer.allocate(resourcePageSize);
        }
        catch(OutOfMemoryError e) {
            return null;
        }
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public boolean hasFreePages() {
        if (freePages.isEmpty()) {
            refill();
        }
        return !freePages.isEmpty();
    }

    private void refill() {
        if (!atLimit && (memoryUsed + resourcePageSize <= memoryLimit)) {
            ByteBuffer bb = allocatePage();
            if (bb == null) {
                atLimit = true;
            }
            else {
                memoryUsed += resourcePageSize;
                
                resourcePages.add(bb);
                int n = resourcePageSize / pageSize;
                for(int i = 0; i != n; ++i) {
                    int offs = i * pageSize;
                    bb.limit(offs + pageSize);
                    bb.position(offs);
                    ByteBuffer sb = bb.slice();
                    BW bw = new BW(sb);
                    freePages.add(bw);
                    allPages.put(sb, bw);
                }
            }
        }
    }

    @Override
    public ByteBuffer accurePage() throws NoMorePagesException {
        if (freePages.isEmpty()) {
            refill();
        }
        if (freePages.isEmpty()) {
            throw new NoMorePagesException();
        }
        Iterator<BW> it = freePages.iterator();
        ByteBuffer bb = it.next().pageBuffer;
        it.remove();
        return bb;
    }

    @Override
    public void releasePage(ByteBuffer bb) {
        BW bw = allPages.get(bb);
        if (bw == null) {
            throw new IllegalStateException("Buffer does not belong ot pool");
        }
        freePages.add(bw);
    }
    
    // need to use wrapper to suppress buffer equality
    private static class BW {
        ByteBuffer pageBuffer;
        
        public BW(ByteBuffer bb) {
            pageBuffer = bb;
        }
    }
}
