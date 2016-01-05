package org.gridkit.jvmtool.heapdump;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.netbeans.lib.profiler.heap.AbstractPagedHprofByteBuffer;

public class PagedFileHprofByteBuffer extends AbstractPagedHprofByteBuffer {

    private final RandomAccessFile file;
    
    public PagedFileHprofByteBuffer(RandomAccessFile file, int pageSize, int pageLimit) throws IOException {
        super(pageSize, pageLimit);
        this.file = file;
        setLength(file.length());
        init();
    }

    @Override
    protected int readPage(long offset, byte[] page, int pageOffset, int len) {
        try {
            file.seek(offset);
            int n = file.read(page, pageOffset, len);
            if (n < 0) {
                n = 0;
            }
            return n;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String toString() {
        return "Multi buffer, random access file strategy";
    }
}
