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

import org.gridkit.jvmtool.heapdump.io.PagedVirtualMemory;

/**
 * Generic {@link HprofByteBuffer} implementation
 * using custom buffering strategy.
 * <br/>
 * Intended to be used with compressed heap dump
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public abstract class AbstractPagedHprofByteBuffer extends HprofByteBuffer {

    protected final PagedVirtualMemory pagedMemory;
    
    public AbstractPagedHprofByteBuffer(PagedVirtualMemory pagedMemory) {
        this.pagedMemory = pagedMemory;
    }

    protected void init() throws IOException {
        readHeader();
    }
    
    protected void setLength(long len) {
        length = len;
        pagedMemory.setLimit(len);
    }
    
    @Override
    char getChar(long index) {
        return pagedMemory.readChar(index);
    }

    @Override
    double getDouble(long index) {
        return pagedMemory.readDouble(index);
    }

    @Override
    float getFloat(long index) {
        return pagedMemory.readFloat(index);
    }

    @Override
    int getInt(long index) {
        return pagedMemory.readInt(index);
    }

    @Override
    long getLong(long index) {
        return pagedMemory.readLong(index);
    }

    @Override
    short getShort(long index) {
        return pagedMemory.readShort(index);
    }

    @Override
    byte get(long index) {
        return pagedMemory.readByte(index);
    }

    @Override
    void get(long position, byte[] chars) {
        pagedMemory.readBytes(position, chars);
    }
}
