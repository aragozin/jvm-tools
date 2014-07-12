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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Tomas Hurka
 */
abstract class AbstractLongMap {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    abstract class Entry {
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final int VALUE_SIZE;
    final int ENTRY_SIZE;
    private File tempFile;
    long fileSize;
    private long keys;
    final int KEY_SIZE;
    final int ID_SIZE;
    final int FOFFSET_SIZE;
    Data dumpBuffer;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    AbstractLongMap(int size,int idSize,int foffsetSize,int valueSize) throws FileNotFoundException, IOException {
        assert idSize == 4 || idSize == 8;
        assert foffsetSize == 4 || foffsetSize == 8;
        keys = (size * 4L) / 3L;
        ID_SIZE = idSize;
        FOFFSET_SIZE = foffsetSize;
        KEY_SIZE = ID_SIZE;
        VALUE_SIZE = valueSize;
        ENTRY_SIZE = KEY_SIZE + VALUE_SIZE;
        fileSize = keys * ENTRY_SIZE;
        tempFile = File.createTempFile("NBProfiler", ".map"); // NOI18N

        RandomAccessFile file = new RandomAccessFile(tempFile, "rw"); // NOI18N
        if (Boolean.getBoolean("org.netbeans.lib.profiler.heap.zerofile")) {    // NOI18N
            byte[] zeros = new byte[512*1024];
            while(file.length()<fileSize) {
                file.write(zeros);
            }
            file.write(zeros,0,(int)(fileSize-file.length()));
        }
        file.setLength(fileSize);
        setDumpBuffer(file);
        tempFile.deleteOnExit();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected void finalize() throws Throwable {
        tempFile.delete();
        super.finalize();
    }

    Entry get(long key) {
        long index = getIndex(key);

        while (true) {
            long mapKey = getID(index);

            if (mapKey == key) {
                return createEntry(index);
            }

            if (mapKey == 0L) {
                return null;
            }

            index = getNextIndex(index);
        }
    }

    Entry put(long key, long value) {
        long index = getIndex(key);

        while (true) {
            if (getID(index) == 0L) {
                putID(index, key);
                return createEntry(index,value);
            }

            index = getNextIndex(index);
        }
    }

    private void setDumpBuffer(RandomAccessFile file) throws IOException {
        long length = file.length();

        try {
            if (length > Integer.MAX_VALUE) {
                dumpBuffer = new LongMemoryMappedData(file, length);
            } else {
                dumpBuffer = new MemoryMappedData(file, length);
            }
        } catch (IOException ex) {
            if (ex.getCause() instanceof OutOfMemoryError) {
                dumpBuffer = new FileData(file, length);
            } else {
                throw ex;
            }
        }
    }

    long getID(long index) {
        if (ID_SIZE == 4) {
            return ((long)dumpBuffer.getInt(index)) & 0xFFFFFFFFL;
        }
        return dumpBuffer.getLong(index);
    }

    void putID(long index,long key) {
        if (ID_SIZE == 4) {
            dumpBuffer.putInt(index,(int)key);
        } else {
            dumpBuffer.putLong(index,key);
        }
    }

    long getFoffset(long index) {
        if (FOFFSET_SIZE == 4) {
            return dumpBuffer.getInt(index);
        }
        return dumpBuffer.getLong(index);
    }

    void putFoffset(long index,long key) {
        if (FOFFSET_SIZE == 4) {
            dumpBuffer.putInt(index,(int)key);
        } else {
            dumpBuffer.putLong(index,key);
        }
    }

    private long getIndex(long key) {
        long hash = key & 0x7FFFFFFFFFFFFFFFL;
        return (hash % keys) * ENTRY_SIZE;
    }

    private long getNextIndex(long index) {
        index += ENTRY_SIZE;
        if (index >= fileSize) {
            index = 0;
        }
        return index;
    }

    private static boolean isLinux() {
        String osName = System.getProperty("os.name");  // NOI18N

        return osName.endsWith("Linux"); // NOI18N
    }

    abstract Entry createEntry(long index);

    abstract Entry createEntry(long index,long value);

    interface Data {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        byte getByte(long index);

        int getInt(long index);

        long getLong(long index);

        void putByte(long index, byte data);

        void putInt(long index, int data);

        void putLong(long index, long data);
    }

    private class FileData implements Data {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        RandomAccessFile file;
        byte[] buf;
        boolean bufferModified;
        long offset;
        final static int BUFFER_SIZE = 128;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        FileData(RandomAccessFile f, long length) throws IOException {
            file = f;
            buf = new byte[ENTRY_SIZE*BUFFER_SIZE];
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public synchronized byte getByte(long index) {
            int i = loadBufferIfNeeded(index);
            return buf[i];
        }

        public synchronized int getInt(long index) {
            int i = loadBufferIfNeeded(index);
            int ch1 = ((int) buf[i++]) & 0xFF;
            int ch2 = ((int) buf[i++]) & 0xFF;
            int ch3 = ((int) buf[i++]) & 0xFF;
            int ch4 = ((int) buf[i]) & 0xFF;

            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        }

        public synchronized long getLong(long index) {
           int i = loadBufferIfNeeded(index);
           return (((long)buf[i++] << 56) +
                  ((long)(buf[i++] & 255) << 48) +
                  ((long)(buf[i++] & 255) << 40) +
                  ((long)(buf[i++] & 255) << 32) +
                  ((long)(buf[i++] & 255) << 24) +
                  ((buf[i++] & 255) << 16) +
                  ((buf[i++] & 255) <<  8) +
                  ((buf[i++] & 255) <<  0));
        }

        public synchronized void putByte(long index, byte data) {
            int i = loadBufferIfNeeded(index);
            buf[i] = data;
            bufferModified = true;
        }

        public synchronized void putInt(long index, int data) {
            int i = loadBufferIfNeeded(index);
            buf[i++] = (byte) (data >>> 24);
            buf[i++] = (byte) (data >>> 16);
            buf[i++] = (byte) (data >>> 8);
            buf[i++] = (byte) (data >>> 0);
            bufferModified = true;
        }

        public synchronized void putLong(long index, long data) {
            int i = loadBufferIfNeeded(index);
            buf[i++] = (byte) (data >>> 56);
            buf[i++] = (byte) (data >>> 48);
            buf[i++] = (byte) (data >>> 40);
            buf[i++] = (byte) (data >>> 32);
            buf[i++] = (byte) (data >>> 24);
            buf[i++] = (byte) (data >>> 16);
            buf[i++] = (byte) (data >>> 8);
            buf[i++] = (byte) (data >>> 0);
            bufferModified = true;
        }

        private int loadBufferIfNeeded(long index) {
            int i = (int) (index % (ENTRY_SIZE * BUFFER_SIZE));
            long newOffset = index - i;

            if (offset != newOffset) {
                try {
                    if (bufferModified) {
                        file.seek(offset);
                        file.write(buf,0,getBufferSize(offset));
                        bufferModified = false;
                    }

                    file.seek(newOffset);
                    file.readFully(buf,0,getBufferSize(newOffset));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                offset = newOffset;
            }

            return i;
        }

        private int getBufferSize(long off) {
            int size = buf.length;

            if (fileSize-off<buf.length) {
                size = (int)(fileSize-off);
            }
            return size;
        }

    }

    private static class MemoryMappedData implements Data {

        private static final FileChannel.MapMode MAP_MODE = isLinux() ? FileChannel.MapMode.PRIVATE : FileChannel.MapMode.READ_WRITE;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        MappedByteBuffer buf;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        MemoryMappedData(RandomAccessFile file, long length)
                  throws IOException {
            FileChannel channel = file.getChannel();
            buf = channel.map(MAP_MODE, 0, length);
            channel.close();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public byte getByte(long index) {
            return buf.get((int) index);
        }

        public int getInt(long index) {
            return buf.getInt((int) index);
        }

        public long getLong(long index) {
            return buf.getLong((int) index);
        }

        public void putByte(long index, byte data) {
            buf.put((int) index, data);
        }

        public void putInt(long index, int data) {
            buf.putInt((int) index, data);
        }

        public void putLong(long index, long data) {
            buf.putLong((int) index, data);
        }
    }

    private static class LongMemoryMappedData implements Data {

        private static int BUFFER_SIZE_BITS = 30;
        private static long BUFFER_SIZE = 1L << BUFFER_SIZE_BITS;
        private static int BUFFER_SIZE_MASK = (int) ((BUFFER_SIZE) - 1);
        private static int BUFFER_EXT = 32 * 1024;

        //~ Instance fields ----------------------------------------------------------------------------------------------------------

        private MappedByteBuffer[] dumpBuffer;


        //~ Constructors ---------------------------------------------------------------------------------------------------------

        LongMemoryMappedData(RandomAccessFile file, long length)
                  throws IOException {
            FileChannel channel = file.getChannel();
            dumpBuffer = new MappedByteBuffer[(int) (((length + BUFFER_SIZE) - 1) / BUFFER_SIZE)];

            for (int i = 0; i < dumpBuffer.length; i++) {
                long position = i * BUFFER_SIZE;
                long size = Math.min(BUFFER_SIZE + BUFFER_EXT, length - position);
                dumpBuffer[i] = channel.map(MemoryMappedData.MAP_MODE, position, size);
            }

            channel.close();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public byte getByte(long index) {
            return dumpBuffer[getBufferIndex(index)].get(getBufferOffset(index));
        }

        public int getInt(long index) {
            return dumpBuffer[getBufferIndex(index)].getInt(getBufferOffset(index));
        }

        public long getLong(long index) {
            return dumpBuffer[getBufferIndex(index)].getLong(getBufferOffset(index));
        }

        public void putByte(long index, byte data) {
            dumpBuffer[getBufferIndex(index)].put(getBufferOffset(index),data);
        }

        public void putInt(long index, int data) {
            dumpBuffer[getBufferIndex(index)].putInt(getBufferOffset(index),data);
        }

        public void putLong(long index, long data) {
            dumpBuffer[getBufferIndex(index)].putLong(getBufferOffset(index),data);
        }

        private int getBufferIndex(long index) {
            return (int) (index >> BUFFER_SIZE_BITS);
        }

        private int getBufferOffset(long index) {
            return (int) (index & BUFFER_SIZE_MASK);
        }
    }

}
