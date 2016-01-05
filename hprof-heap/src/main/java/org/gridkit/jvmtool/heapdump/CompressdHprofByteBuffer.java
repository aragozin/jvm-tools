package org.gridkit.jvmtool.heapdump;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.gridkit.gzrand.RandomAccessFileInputStream;
import org.gridkit.gzrand.RandomAccessGZipFile;
import org.gridkit.gzrand.RandomAccessInputStream;
import org.netbeans.lib.profiler.heap.AbstractPagedHprofByteBuffer;

public class CompressdHprofByteBuffer extends AbstractPagedHprofByteBuffer {

    private final RandomAccessInputStream index;
    
    public CompressdHprofByteBuffer(RandomAccessFile file, int pageSize, int pageLimit) throws IOException {
        super(pageSize, pageLimit);
        RandomAccessFileInputStream seeker = new RandomAccessFileInputStream(file);
        RandomAccessGZipFile zfile = new RandomAccessGZipFile(seeker, pageSize);
        index = zfile;
        setLength(zfile.length());
        init();
    }

    @Override
    protected int readPage(long offset, byte[] page, int pageOffset, int len) {
//        System.out.println("Load page: " + Long.toHexString(offset));
        try {
            index.seek(offset);
            int pos = pageOffset;
            int rem = len;
            while(rem > 0) {
                int n = index.read(page, pos, rem);
                if (n < 0) {
                    return len - rem;
                }
                rem -= n;
                pos += n;
            }
            return len;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
