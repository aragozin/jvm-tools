/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.heap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tomas Hurka
 */
class NumberList {

    private static final int NUMBERS_IN_BLOCK = 3;
    private final File dataFile;
    private final RandomAccessFile data;
    private final int numberSize;
    private final int blockSize;
    private final Map/*offset,block*/ blockCache;
    private final Set dirtyBlocks;
    private long blocks;
    private MappedByteBuffer buf;
    private long mappedSize;

    NumberList(long dumpFileSize) throws IOException {
        this(bytes(dumpFileSize));
    }

    NumberList(int elSize) throws IOException {
        dataFile = File.createTempFile("NBProfiler", ".ref"); // NOI18N
        data = new RandomAccessFile(dataFile, "rw"); // NOI18N
        numberSize = elSize;
        blockCache = new BlockLRUCache();
        dirtyBlocks = new HashSet(100000);
        blockSize = (NUMBERS_IN_BLOCK + 1) * numberSize;
        dataFile.deleteOnExit();
        addBlock(); // first block is unused, since it starts at offset 0
    }

    private static int bytes(long number) {
        if ((number & ~0xFFL) == 0L) {
            return 1;
        }
        if ((number & ~0xFFFFL) == 0L) {
            return 2;
        }
        if ((number & ~0xFFFFFFL) == 0L) {
            return 3;
        }
        if ((number & ~0xFFFFFFFFL) == 0L) {
            return 4;
        }
        if ((number & ~0xFFFFFFFFFFL) == 0L) {
            return 5;
        }
        if ((number & ~0xFFFFFFFFFFFFL) == 0L) {
            return 6;
        }
        if ((number & ~0xFFFFFFFFFFFFFFL) == 0L) {
            return 7;
        }
        return 8;
    }

    protected void finalize() throws Throwable {
        dataFile.delete();
        super.finalize();
    }

    long addNumber(long startOffset,long number) throws IOException {
        int slot;
        byte[] block = getBlock(startOffset);
        for (slot=0;slot<NUMBERS_IN_BLOCK;slot++) {
            long el = readNumber(block,slot);
            if (el == 0L) {
                writeNumber(startOffset,block,slot,number);
                return startOffset;
            }
            if (el == number) { // number is already in the list
                return startOffset; // do nothing
            }
        }
        long nextBlock = addBlock(); // create next blok
        block = getBlock(nextBlock);
        writeNumber(nextBlock,block,slot,startOffset); // put next block in front of old block
        writeNumber(nextBlock,block,0,number); // write number to first position in the new block
        return nextBlock;
    }

    long addFirstNumber(long number1,long number2) throws IOException {
        long blockOffset = addBlock();
        byte[] block = getBlock(blockOffset);
        writeNumber(blockOffset,block,0,number1);
        writeNumber(blockOffset,block,1,number2);
        return blockOffset;
    }

    void putFirst(long startOffset,long number) throws IOException {
        int slot;
        long offset = startOffset;
        long movedNumber = 0;
        for(;;) {
            byte[] block = getBlock(offset);
            for (slot=0;slot<NUMBERS_IN_BLOCK;slot++) {
                long el = readNumber(block,slot);
                if (offset == startOffset && slot == 0) { // first block
                    if (number == el) { // already first element
                        return;
                    }
                    movedNumber = el;
                    writeNumber(offset,block,slot,number);
                } else if (el == 0L) { // end of the block, move to next one
                    break;
                } else if (el == number) { // number is already in the list
                    writeNumber(offset,block,slot,movedNumber);    // replace number and return
                    return;
                }
            }
            offset = getOffsetToNextBlock(block);
            if (offset == 0L) {
                System.out.println("Error - number not found at end");
                return;
            }
        }
    }

    long getFirstNumber(long startOffset) throws IOException {
        byte[] block = getBlock(startOffset);
        return readNumber(block,0);
    }

    List getNumbers(long startOffset) throws IOException {
        int slot;
        List numbers = new ArrayList();

        for(;;) {
            byte[] block = getBlock(startOffset);
            for (slot=0;slot<NUMBERS_IN_BLOCK;slot++) {
                long el = readNumber(block,slot);
                if (el == 0L) {     // end of the block, move to next one
                    break;
                }
                numbers.add(new Long(el));
            }
            long nextBlock = getOffsetToNextBlock(block);
            if (nextBlock == 0L) {
                return numbers;
            }
            startOffset = nextBlock;
        }
    }

    private void mmapData() {
        if (buf == null && blockSize*blocks < Integer.MAX_VALUE) {
            try {
                buf = data.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, data.length());
                mappedSize = blockSize*blocks;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    void flush() {
        try {
            flushDirtyBlocks();
            blockCache.clear();
            mmapData();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private long getOffsetToNextBlock(byte[] block) {
        return readNumber(block,NUMBERS_IN_BLOCK);
    }

    private long readNumber(byte[] block,int slot) {
        int offset = slot*numberSize;
        long el = 0;
//        for (int i=0;i<numberSize;i++) {
//            el <<= 8;
//            el |= ((int)block[offset+i]) & 0xFF;
//        }
        if (numberSize == 4) {
            return ((long)getInt(block,offset)) & 0xFFFFFFFFL;
        } else if (numberSize == 8) {
            return getLong(block,offset);
        }
        return el;
    }

    private int getInt(byte[] buf, int i) {
        int ch1 = ((int) buf[i++]) & 0xFF;
        int ch2 = ((int) buf[i++]) & 0xFF;
        int ch3 = ((int) buf[i++]) & 0xFF;
        int ch4 = ((int) buf[i]) & 0xFF;

        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    private long getLong(byte[] buf, int i) {
       return (((long)buf[i++] << 56) +
              ((long)(buf[i++] & 255) << 48) +
              ((long)(buf[i++] & 255) << 40) +
              ((long)(buf[i++] & 255) << 32) +
              ((long)(buf[i++] & 255) << 24) +
              ((buf[i++] & 255) << 16) +
              ((buf[i++] & 255) <<  8) +
              ((buf[i++] & 255) <<  0));
    }

    private void writeNumber(long blockOffset,byte[] block,int slot,long element) throws IOException {
        if (blockOffset < mappedSize) {
            long offset = blockOffset+slot*numberSize;
            buf.position((int)offset);
            for (int i=numberSize-1;i>=0;i--) {
                byte el = (byte)(element >> (i*8));

                buf.put(el);
            }
        } else {
            Long offsetObj = new Long(blockOffset);
            int offset = slot*numberSize;
            for (int i=numberSize-1;i>=0;i--) {
                byte el = (byte)(element >> (i*8));
                block[offset++]=el;
            }
            dirtyBlocks.add(offsetObj);
            if (dirtyBlocks.size()>10000) {
                flushDirtyBlocks();
            }
        }
    }

    private byte[] getBlock(long offset) throws IOException {
        byte[] block;
        if (offset < mappedSize) {
            block = new byte[blockSize];
            buf.position((int)offset);
            buf.get(block);
            return block;
        } else {
            Long offsetObj = new Long(offset);

            block = (byte[]) blockCache.get(offsetObj);
            if (block == null) {
                block = new byte[blockSize];
                data.seek(offset);
                data.readFully(block);
                blockCache.put(offsetObj,block);
            }
            return block;
        }
    }

    private long addBlock() throws IOException {
        long offset=blocks*blockSize;
        blockCache.put(new Long(offset),new byte[blockSize]);
        blocks++;
        return offset;
    }

    private void flushDirtyBlocks() throws IOException {
        if (dirtyBlocks.isEmpty()) {
            return;
        }
        Long[] dirty=new Long[dirtyBlocks.size()];
        dirtyBlocks.toArray(dirty);
        Arrays.sort(dirty);
        byte blocks[] = new byte[1024*blockSize];
        int dataOffset = 0;
        long lastBlockOffset = 0;
        for(int i=0;i<dirty.length;i++) {
            Long blockOffsetLong = dirty[i];
            byte[] block = (byte[]) blockCache.get(blockOffsetLong);
            long blockOffset = blockOffsetLong.longValue();
            if (lastBlockOffset+dataOffset==blockOffset && dataOffset <= blocks.length - blockSize) {
                System.arraycopy(block,0,blocks,dataOffset,blockSize);
                dataOffset+=blockSize;
            } else {
                data.seek(lastBlockOffset);
                data.write(blocks,0,dataOffset);
                dataOffset = 0;
                System.arraycopy(block,0,blocks,dataOffset,blockSize);
                dataOffset+=blockSize;
                lastBlockOffset = blockOffset;
            }
        }
        data.seek(lastBlockOffset);
        data.write(blocks,0,dataOffset);
        dirtyBlocks.clear();
    }

    private class BlockLRUCache extends LinkedHashMap {

        private static final int MAX_CAPACITY = 10000;

        private BlockLRUCache() {
            super(MAX_CAPACITY,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size()>MAX_CAPACITY) {
                Object key = eldest.getKey();
                if (!dirtyBlocks.contains(key)) {
                    return true;
                }
                get(key);
            }
            return false;
        }

    }
}
