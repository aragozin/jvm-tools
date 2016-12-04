package org.gridkit.jvmtool.heapdump.io;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.gridkit.jvmtool.heapdump.io.PagePool.NoMorePagesException;

/**
 * <p>
 * This class manages pages and their mapping
 * to virtual address space.
 * </p>
 * <p>
 * It offers basic memory access utilities along side
 * with page fault management and hot page accounting.
 * </p>
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public abstract class PagedVirtualMemory {

    private static final long SANITY_LIMIT = 1l << 40; // one terra byte

    private static final int LOAD_BUST = 16;
    private static final int MAP_BUST = 4;
    
    private final PageComparator cmp = new PageComparator(); 
    protected final PagePool pagePool;
    protected final int pageSize;
    protected final int pageBits;
    protected final long pageMask;
    
    // array below are indexed by virtual pageId
    // buffers and pageMap are sparse
    private ByteBuffer[] bufferMap = new ByteBuffer[0];
    private int[] hitCounts = new int[0];
    private PageInfo[] pageMap = new PageInfo[0];
    
    private long pageFaults = 0;
    private long eof = SANITY_LIMIT;
    
    // this is dense collection of allocated pages
    private List<PageInfo> pages = new ArrayList<PageInfo>();
    
    private int mappedPageCount = 0;
    private int mappedPageLimit = 0;
    
    public PagedVirtualMemory(PagePool pagePool) {
        this.pagePool = pagePool;
        this.pageSize = pagePool.getPageSize();
        if (Integer.bitCount(pageSize) != 1) {
            throw new IllegalArgumentException("Page size should be power of 2 (" + pageSize + ")");
        }
        pageMask = pageSize - 1;
        pageBits = Long.bitCount(pageMask);
    }

    public void setLimit(long vsize) {
        eof = vsize;
        int n = (int) (vsize >> pageBits);
        if (n >= bufferMap.length) {
            int nsize = Math.max(n + 1, (3 * bufferMap.length) / 4);
            bufferMap = Arrays.copyOf(bufferMap, nsize);
            hitCounts = Arrays.copyOf(hitCounts, nsize);
            pageMap = Arrays.copyOf(pageMap, nsize);
        }
    }
    
    public char readChar(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getChar((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[2];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getChar();
        }
    }

    public double readDouble(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getDouble((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[8];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getDouble();
        }
    }

    public float readFloat(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getFloat((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[4];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getFloat();
        }
    }

    public int readInt(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getInt((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[4];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getInt();
            
        } 
//        catch (BufferUnderflowException e) {
//            byte[] sb = new byte[4];
//            readSafe(index, sb, sb.length);
//            return ByteBuffer.wrap(sb).getInt();
//        }
    }

    public long readLong(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getLong((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[8];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getLong();
        }
    }

    public short readShort(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getShort((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[2];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getShort();
        }
    }

    public byte readByte(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.get((int)(index & pageMask));
        } catch(IndexOutOfBoundsException e) {
            if (index >= eof) {
                throw new RuntimeException("Read beyond end of file: " + index);
            }
            else {
                throw e;
            }
        }
    }

    public void readBytes(long position, byte[] chars) {
        try {
            ByteBuffer buf = ensureBuffer(position);
            buf.position((int)(position & pageMask));
            buf.get(chars);
            buf.position(0);
        } catch (IndexOutOfBoundsException e) {
            readSafe(position, chars, chars.length);
        } catch (BufferUnderflowException e) {
            readSafe(position, chars, chars.length);
        }
    }

    private void readSafe(long index, byte[] buffer, int len) {
        // TODO may be very slow
        for(int i = 0; i != len; ++i) {
            buffer[i] = readByte(index + i);
        }
    }

    private ByteBuffer ensureBuffer(long index) {
        if (index > SANITY_LIMIT) {
            throw new IllegalArgumentException("Offset beyond sanity: " + index);
        }
        int n = (int) (index >> pageBits);
        if (n >= bufferMap.length) {
            int nsize = Math.max(n + 1, (3 * bufferMap.length) / 4);
            bufferMap = Arrays.copyOf(bufferMap, nsize);
            hitCounts = Arrays.copyOf(hitCounts, nsize);
            pageMap = Arrays.copyOf(pageMap, nsize);
        }
        ByteBuffer b = bufferMap[n];
        if (b == null) {
            pageFault(n);
            b = bufferMap[n];
            if (b == null) {
                throw new IllegalArgumentException("Failed to load page #" + n);
            }
        }
        return b;
    }
    
    protected PageInfo allocPage() {
        ByteBuffer buf = allocBuffer();
        if (buf != null) {
            PageInfo pi = new PageInfo(pages.size());            
            pi.buffer = buf;
            pages.add(pi);
            mappedPageLimit = pages.size() / 2; // why?
            return pi;
        }
        else {
            return null;
        }
    }

    protected ByteBuffer allocBuffer() {
        if (pagePool.hasFreePages()) {
            try {
                ByteBuffer buf = pagePool.accurePage();
                return buf;
            } catch (NoMorePagesException e) {
                return null;
            }
        }
        else {
            return null;
        }
    }
    
    protected void reclaimPages(PageInfo[] pages) {
        EvictBuffer buffer = new EvictBuffer(pages);
        while(buffer.size() < pages.length) {
            PageInfo pi = allocPage();
            if (pi != null) {
                buffer.push(pi);
            }     
            else {
                break;
            }
        }
        if (buffer.size() < pages.length) {
            for(PageInfo pi: this.pages) {
                buffer.push(pi);
            }
        }
        if (buffer.size() < pages.length) {
            throw new RuntimeException("Cannot reclaim " + pages.length);
        }
        
        // unlink reclaimed pages
        for(PageInfo pi: pages) {
            if (pi.pageIndex >= 0) {
                bufferMap[pi.pageIndex] = null;
                pageMap[pi.pageIndex] = null;
                pi.pageIndex = -1;
            }
            pi.age = -1;
        }
        // done
    }
    
    protected abstract void loadPage(int pageId);
    
    protected boolean isPageMapped(int pageId) {
        return pageId < pageMap.length && pageMap[pageId] != null;
    }
    
    protected void mapPage(int pageId, PageInfo info) {
        if (pageMap[pageId] != null) {
            throw new IllegalArgumentException("Page is already mapped");
        }
        info.age = pageFaults;
        info.pageIndex = pageId;
        pageMap[pageId] = info;
        hitCounts[pageId] += MAP_BUST; // fake hit
    }
    
    protected void pageFault(int pageId) {
        ++hitCounts[pageId];
        if (pageMap[pageId] != null && mappedPageCount < mappedPageLimit) {
            // soft fall - restore mapping
            bufferMap[pageId] = pageMap[pageId].buffer;
            return;
        }
        // otherwise process hard fall
        ++pageFaults;
        if (pageFaults % (1 << 10) == 0) {
            fadeHitCounts();
        }
        
        // we hit mapping threshold need to unmap pages
        if (mappedPageCount >= mappedPageLimit) {
            // unmap all pages to free limit
            Arrays.fill(bufferMap, null);
            mappedPageCount = 0;
        }
        
        if (pageMap[pageId] != null) {
            bufferMap[pageId] = pageMap[pageId].buffer;
            ++mappedPageCount;
            return;            
        }
        
        loadPage(pageId);
        hitCounts[pageId] += LOAD_BUST; // bust loaded page
        
        if (pageMap[pageId] != null) {
            bufferMap[pageId] = pageMap[pageId].buffer;
            ++mappedPageCount;
            return;            
        }
    }
    
    private void fadeHitCounts() {
        for(int i = 0; i != hitCounts.length; ++i) {
            hitCounts[i] = hitCounts[i] / 2;
        }
    }

//    private PageInfo compare(PageInfo page1, PageInfo page2) {
//        if (page1 == null) {
//            return page2;
//        }
//        else if (page2 == null) {
//            return page1;
//        }
//        int h1 = page1.pageIndex < 0 ? 0 : hitCounts[page1.pageIndex];
//        int h2 = page2.pageIndex < 0 ? 0 : hitCounts[page2.pageIndex];
//        if (h1 > h2) {
//            return page2;
//        }
//        else if (h2 > h1) {
//            return page1;
//        }
//        if (page1.age < page2.age) {
//            return page1;
//        }
//        else if (page2.age < page1.age) {
//            return page2;
//        }
//        return page1.pageIndex < page2.pageIndex ? page1 : page2;
//    }

    protected static class PageInfo {

        protected ByteBuffer buffer;
        int pageNo;
        int pageIndex = -1;
        long age = -1;
        
        public PageInfo(int pageNo) {
            this.pageNo = pageNo;
        }
        
        public String toString() {
            return "@" + pageIndex + " A" + age;
        }
    }
    
    class EvictBuffer {
        
        PageInfo[] array;
        int size;
        
        EvictBuffer(PageInfo[] array) {
            this.array = array;
        }
        
        public int size() {
            return size;
        }

        public void push(PageInfo pi) {
            if (size == 0) {
                array[0] = pi;
                ++size;
            }
            else {
                int n;
                for(n = size - 1;n >= 0; --n) {
                    if (cmp.compare(pi, array[n]) >= 0) {
                        break;
                    }
                }
                n = n + 1;
                if (n < array.length) {
                    insert(n, pi);
                }
            }
        }
        
        private void insert(int pos, PageInfo pi) {
            if (pos < array.length - 1) {
                System.arraycopy(array, pos, array, pos + 1, array.length - pos - 1);
            }
            array[pos] = pi;
            if (size < array.length) {
                ++size;
            }
        }
    }
    
    private class PageComparator implements Comparator<PageInfo> {

        @Override
        public int compare(PageInfo page1, PageInfo page2) {
            int h1 = page1.pageIndex < 0 ? 0 : hitCounts[page1.pageIndex];
            int h2 = page2.pageIndex < 0 ? 0 : hitCounts[page2.pageIndex];
            if (h1 > h2) {
                return 1;
            }
            else if (h2 > h1) {
                return -1;
            }
            if (page1.age < page2.age) {
                return 1;
            }
            else if (page2.age < page1.age) {
                return -1;
            }
            return page1.pageIndex < page2.pageIndex ? 1 
                    : page1.pageIndex == page2.pageIndex ? 0
                    : -1;
        }
    }    
}
