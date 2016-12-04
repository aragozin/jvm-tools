package org.gridkit.jvmtool.heapdump.io;

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class PagedVirtualMemoryTest {

    @Test
    public void singleRun() {
        
        TestMemory mem = new TestMemory();
        mem.setLimit(16 << 10);
        
        for(int i = 0; i != 16; ++i) {
            verify(mem, i << 10, 1 << 10);
        }
        
        System.out.println(mem.getFaultCount());
    }

    @Test
    public void randomAccessRun() {
        
        TestMemory mem = new TestMemory();
        int limit = 16 << 10;
        mem.setLimit(limit);
        Random rnd = new Random();
        
        for(int i = 0; i != 10000; ++i) {
            long n = rnd.nextInt(limit - 64);
            
            verify(mem, n, 64);
        }
        
        System.out.println(mem.getFaultCount());
    }
    
    public void verify(TestMemory mem, long offset, int len) {
        Random rnd = new Random();
        long n = offset;
        int failCount = 0;
        for(int i = 0; i != len; ++i) {
            rnd.setSeed(n);
            int e = 0xFF & rnd.nextInt();
            int a = 0xFF & ((int)mem.readByte(n));
            if (a != e) {
                ++failCount;
                System.out.println("[" + n + "] " + a + " != " + e);
            }
            ++n;
        }
        if (failCount > 0) {
            Assert.fail("Memory mismatch " + failCount + " bytes");
        }
    }
    
    
    private class TestMemory extends SimplePagedVirtualMemory {

        private long faultCount;
        
        public TestMemory() {
            super(new ByteBufferPageManager(64, 4096, 4096));
        }

        public long getFaultCount() {
            return faultCount;
        }
        
        @SuppressWarnings("unused")
        public void resetFaultCount() {
            faultCount = 0;
        }
        
        @Override
        public int readPage(long offset, ByteBuffer page) {
            ++faultCount;
            
            Random rnd = new Random();
            
            long n = offset;
            while(page.remaining() > 0) {
                rnd.setSeed(n);
                page.put((byte) rnd.nextInt());
                ++n;
            }
            
            return (int) (n - offset);
        }
    }    
}
