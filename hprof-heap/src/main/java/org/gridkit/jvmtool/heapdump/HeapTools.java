/**
 * Copyright 2016 Alexey Ragozin
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
package org.gridkit.jvmtool.heapdump;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;

import org.netbeans.lib.profiler.heap.FastHprofHeap;
import org.netbeans.lib.profiler.heap.Heap;

public class HeapTools {

    /**
     * Open heap dump in HPROF format. Supports plain or gzip compressed dumps.
     * @throws IOException
     */
    public static Heap openHeapDump(File file) throws IOException {
        if (isArchive(file)) {
            CompressdHprofByteBuffer buffer = new CompressdHprofByteBuffer(new RandomAccessFile(file, "r"), 8 << 20, 16);
            return new FastHprofHeap(buffer, 0);
        }
        else {
            return new FastHprofHeap(file, 0);
        }
    }

    private static boolean isArchive(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            try {
                GZIPInputStream gis = new GZIPInputStream(fis);
                gis.read();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        finally {
            try {
                fis.close();
            } catch (IOException e) {
            }
        }
    }    
}
