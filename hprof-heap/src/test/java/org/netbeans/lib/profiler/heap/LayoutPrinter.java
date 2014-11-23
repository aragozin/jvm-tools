package org.netbeans.lib.profiler.heap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

/**
 * Resolves offset by instance ID.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class LayoutPrinter {

    HprofHeap heap;
    HprofByteBuffer dumpBuffer;
    long[] pointer = new long[1];

    public void scan(String path) throws FileNotFoundException, IOException {
        FastHprofHeap heap = new FastHprofHeap(new File(path), 0);
        this.heap = heap;
        this.dumpBuffer = heap.dumpBuffer;

        TagBounds bounds = heap.getAllInstanceDumpBounds();
        pointer[0] = bounds.startOffset;
        long offs = pointer[0];
        long lastPtr = readID() >>> 3;
        System.out.println("" + offs + " -> " + lastPtr);
        while(true) {
            offs = pointer[0];
            long ref = readID();
            if (ref == -1) {
                break;
            }
            long ptr = ref >>> 3;
            if (ptr <= lastPtr) {
                System.out.println("" + offs + " -> " + lastPtr);
            }
            lastPtr = ptr;
        }
    }

    @Test
    public void test() throws FileNotFoundException, IOException {
        scan("file path");
    }

    private long readID() {
        TagBounds bounds = heap.getAllInstanceDumpBounds();

        while(pointer[0] < bounds.endOffset) {
            long ptr = pointer[0];
            int tag = heap.readDumpTag(pointer);

            if (   tag == HprofHeap.INSTANCE_DUMP
                || tag == HprofHeap.OBJECT_ARRAY_DUMP
                || tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                return dumpBuffer.getID(ptr + 1);
            }
        }
        return -1;
    }
}
