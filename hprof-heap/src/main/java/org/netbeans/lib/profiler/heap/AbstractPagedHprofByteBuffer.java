/**
 * Copyright 2015 Alexey Ragozin
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
package org.netbeans.lib.profiler.heap;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Generic {@link HprofByteBuffer} implementation
 * using custom buffering strategy.
 * <br/>
 * Intended to be used with compressed heap dump
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public abstract class AbstractPagedHprofByteBuffer extends HprofByteBuffer {

    private static final long SANITY_LIMIT = 1l << 40; // one terra byte
    
    private final int pageSize;
    private final int pageLimit;
    private final int pageBits;
    private final long pageMask;
    
    private ByteBuffer[] buffers = new ByteBuffer[0];
    private int[] hitCounts = new int[0];
    private long pageFaults = 0;
    private long eof = SANITY_LIMIT;
    
    private PageInfo[] pages;
    private int mappedPageCount = 0;
    
    public AbstractPagedHprofByteBuffer(int pageSize, int pageLimit) {
        this.pageLimit = pageLimit;
        if (Integer.bitCount(pageSize) != 1) {
            throw new IllegalArgumentException("Page size should be power of 2 (" + pageSize + ")");
        }
        this.pageSize = pageSize;
        pageMask = pageSize - 1;
        pageBits = Long.bitCount(pageMask);
        pages = new PageInfo[pageLimit];
        // preallocate page, we are gonna need them any way
        for(int i = 0; i != pageLimit; ++i) {
            pages[i] = new PageInfo(pageSize);
            pages[i].pageIndex = -1;
        }
    }

    protected void init() throws IOException {
        readHeader();
    }
    
    protected void setLength(long len) {
        this.length = len;
        this.eof = len;
    }
    
    protected abstract int readPage(long offset, byte[] page, int pageOffset, int len);
    
    @Override
    char getChar(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getChar((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[2];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getChar();
        }
    }

    @Override
    double getDouble(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getDouble((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[8];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getDouble();
        }
    }

    @Override
    float getFloat(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getFloat((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[4];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getFloat();
        }
    }

    @Override
    int getInt(long index) {
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

    @Override
    long getLong(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getLong((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[8];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getLong();
        }
    }

    @Override
    short getShort(long index) {
        try {
            ByteBuffer buf = ensureBuffer(index);
            return buf.getShort((int)(index & pageMask));
        } catch (IndexOutOfBoundsException e) {
            byte[] sb = new byte[2];
            readSafe(index, sb, sb.length);
            return ByteBuffer.wrap(sb).getShort();
        }
    }

    @Override
    byte get(long index) {
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

    @Override
    void get(long position, byte[] chars) {
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
            buffer[i] = get(index + i);
        }
    }

    private ByteBuffer ensureBuffer(long index) {
        if (index > SANITY_LIMIT) {
            throw new IllegalArgumentException("Offset beyond sanity: " + index);
        }
        int n = (int) (index >> pageBits);
        if (n >= buffers.length) {
            int nsize = Math.max(n + 1, (3 * buffers.length) / 4);
            buffers = Arrays.copyOf(buffers, nsize);
            hitCounts = Arrays.copyOf(hitCounts, nsize);
        }
        ByteBuffer b = buffers[n];
        if (b == null) {
            loadBuffer(n);
            b = buffers[n];
        }
        return b;
    }
    
    private void loadBuffer(int n) {
        ++pageFaults;
        PageInfo unmapPage = null;
        PageInfo evictPage = null;
        PageInfo remapPage = null;
        for(PageInfo pi: pages) {
            if (pi.pageIndex < 0) {
                evictPage = pi;
            }
            else {
                if (pi.pageIndex == n) {
                    remapPage = pi;
                }
                else {
                    // page has data
                    if (buffers[pi.pageIndex] != null) {
                        // page is mapped
                        unmapPage = compare(unmapPage, pi);
                    }
                    evictPage = compare(evictPage, pi);
                }
            }
        }
        ++hitCounts[n];
        if (pageFaults % (16 << 10) == 0) {
            fadeHitCounts();
        }
        if (remapPage != null) {
            if (mappedPageCount >= pageLimit / 2) {
                if (unmapPage != null) {
                    buffers[unmapPage.pageIndex] = null;
                    --mappedPageCount;
                }
            }
            buffers[remapPage.pageIndex] = remapPage.buffer;
            ++mappedPageCount;
        }
        else {
            // need to evict/load
            if (evictPage == null) {
                throw new RuntimeException("Eviction page is not found");
            }
            if (evictPage.pageIndex >= 0 && buffers[evictPage.pageIndex] != null) {
                buffers[evictPage.pageIndex] = null;
                --mappedPageCount;
            }
            long offset = ((long)n) << pageBits;
            int realSize = readPage(offset, evictPage.bufferArray, 0, pageSize);
            evictPage.buffer.limit(realSize);
            evictPage.buffer.position(0);
            
            if (realSize != pageSize) {
                eof = Math.min(eof, offset + realSize);
            }
            
            evictPage.pageIndex = n;
            evictPage.age = pageFaults;
            
            buffers[n] = evictPage.buffer;
            ++mappedPageCount;
        }
    }

    private void fadeHitCounts() {
        for(int i = 0; i != hitCounts.length; ++i) {
            hitCounts[i] = hitCounts[i] / 2;
        }
    }

    private PageInfo compare(PageInfo page1, PageInfo page2) {
        if (page1 == null) {
            return page2;
        }
        else if (page2 == null) {
            return page1;
        }
        int h1 = page1.pageIndex < 0 ? 0 : hitCounts[page1.pageIndex];
        int h2 = page1.pageIndex < 0 ? 0 : hitCounts[page2.pageIndex];
        if (h1 > h2) {
            return page2;
        }
        else if (h2 > h1) {
            return page1;
        }
        if (page1.age < page2.age) {
            return page1;
        }
        else if (page2.age < page1.age) {
            return page2;
        }
        return page1.pageIndex < page2.pageIndex ? page1 : page2;
    }

    private static class PageInfo {
        
        ByteBuffer buffer;
        byte[] bufferArray;
        int pageIndex;
        long age;
        
        public PageInfo(int size) {
            bufferArray = new byte[size];
            buffer = ByteBuffer.wrap(bufferArray);
        }
    }
}
