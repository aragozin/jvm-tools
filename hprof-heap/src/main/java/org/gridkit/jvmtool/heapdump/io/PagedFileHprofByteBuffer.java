package org.gridkit.jvmtool.heapdump.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.netbeans.lib.profiler.heap.AbstractPagedHprofByteBuffer;

public class PagedFileHprofByteBuffer extends AbstractPagedHprofByteBuffer {

    public PagedFileHprofByteBuffer(RandomAccessFile file, PagePool pagePool) throws IOException {
        super(new FileBackedVirtualMemory(file, pagePool));
        setLength(file.length());
        init();
    }

    private static class FileBackedVirtualMemory extends SimplePagedVirtualMemory {

        private final FileChannel channel;
        
        public FileBackedVirtualMemory(RandomAccessFile file, PagePool pagePool) {
            super(pagePool);
            this.channel = file.getChannel();
        }
        
        @Override
        protected int readPage(long offset, ByteBuffer bb) {
            try {
                channel.position(offset);
                channel.read(bb);
                bb.flip();
                return bb.remaining();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
    }
    
    @Override
    public String toString() {
        return "Multi buffer, random access file strategy";
    }
}
