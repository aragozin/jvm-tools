package org.gridkit.jvmtool.heapdump.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.gridkit.gzrand.RandomAccessFileInputStream;
import org.gridkit.gzrand.RandomAccessGZipFile;
import org.gridkit.gzrand.RandomAccessInputStream;
import org.netbeans.lib.profiler.heap.AbstractPagedHprofByteBuffer;

public class CompressdHprofByteBuffer extends AbstractPagedHprofByteBuffer {

    private static final int COMPRESSED_PAGE = 16 << 20;
    
    public CompressdHprofByteBuffer(RandomAccessFile file, PagePool pool) throws IOException {
        super(new CompressedMemory(new RandomAccessGZipFile(new RandomAccessFileInputStream(file), COMPRESSED_PAGE), pool, COMPRESSED_PAGE / pool.getPageSize()));
        setLength(((CompressedMemory)pagedMemory).index.length());
        init();
    }

    private static class CompressedMemory extends BulkFetchPagedVirtualMemory {
        
        private final RandomAccessInputStream index;
        private final byte[] readBuffer;

        public CompressedMemory(RandomAccessInputStream index, PagePool pagePool, int prefetch) throws IOException {
            super(pagePool, prefetch);
            this.index = index;
            this.readBuffer = new byte[pagePool.getPageSize()];
            this.setLimit(index.length());
        }

        @Override
        protected int readPage(long offset, ByteBuffer page) {
//            System.out.println("Load page: " + Long.toHexString(offset));
            try {
                index.seek(offset);
                int pos = 0;
                int rem = page.remaining();
                while(rem > 0) {
                    int n = index.read(readBuffer, pos, rem);
                    if (n < 0) {
                        break;
                    }
                    rem -= n;
                    pos += n;
                }
                
                page.put(readBuffer, 0, pos);
                return pos;
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }    
}
