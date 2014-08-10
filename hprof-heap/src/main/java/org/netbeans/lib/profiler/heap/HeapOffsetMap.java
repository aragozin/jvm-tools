/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Alexey Ragozin
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

import java.util.Arrays;

/**
 * Resolves offset by instance ID.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class HeapOffsetMap {

    private final static int DEFAULT_CACHE_SIZE = 1027;
//    private final static int DEFAULT_CACHE_SIZE = 5027;

//    private final int pageSizeBits = 12;
    private final int pageSizeBits = 10;
    private final int pageSize = 1 << pageSizeBits;
    private final int allignment = 8;

    private final long idOffset;
    private long[] offsetMap; // maps IDs to offsets
    private final int[] cachePageId;
    private final int[][] cachePageData;
    private final HprofHeap heap;
    private final HprofByteBuffer dumpBuffer;
    /** used for dump buffer access */
    private final long[] pointer = new long[1];
    private int maxPage = 0; // last scanned ID

    public HeapOffsetMap(HprofHeap heap) {
        this.heap = heap;
        this.dumpBuffer = heap.dumpBuffer;
        TagBounds bounds = heap.getAllInstanceDumpBounds();

        pointer[0] = bounds.startOffset;
        idOffset = readID();
        long span = bounds.endOffset - bounds.startOffset; // rough estimate
        offsetMap = new long[(int)((span + pageSize - 1) / pageSize)];
        offsetMap[0] = bounds.startOffset;

        cachePageId = new int[DEFAULT_CACHE_SIZE];
        cachePageData = new int[cachePageId.length][pageSize];
        Arrays.fill(cachePageId, -1);
    }

    public long offset(long id) {
        long ref = compressID(id);
        int page = (int) (ref / pageSize);
        if (page >= maxPage) {
            scanPage(page);
        }
        int[] shiftMap = getPage(page);
        long baseOffs = offsetMap[page];
        if (baseOffs < 0) {
            throw new IllegalArgumentException("ID is not valid: " + id);
        }
        int shift = shiftMap[(int) (ref % pageSize)];
        if (shift < 0) {
            throw new IllegalArgumentException("ID is not valid: " + id);
        }
        long offs = baseOffs + shift;
        return offs;
    }

    private void scanPage(int page) {
        int n = maxPage;
        while(n <= page) {
            if (offsetMap.length <= n) {
                offsetMap = Arrays.copyOf(offsetMap, page + 1);
            }
            int cslot = n % cachePageId.length;
            cachePageId[cslot] = n;
            readPage(n * pageSize, offsetMap[n], cachePageData[cslot]);
            long noffs = pointer[0];
            long rid = readID();
            if (rid >= 0) {
                long nid = compressID(rid);
                int nn = (int) (nid / pageSize);
                if (offsetMap.length <= nn) {
                    offsetMap = Arrays.copyOf(offsetMap, nn + 16);
                }
                for(int i = n + 1; i < nn; ++i) {
                    offsetMap[i] = -1; // no ids in range
                }
                offsetMap[nn] = noffs;
                maxPage = nn;
                n = nn;
            }
            else {
                // no more pages
                maxPage = n + 1;
                if (offsetMap.length <= maxPage) {
                    offsetMap = Arrays.copyOf(offsetMap, maxPage + 1);
                }
                offsetMap[maxPage] = heap.getAllInstanceDumpBounds().endOffset;
                if (n < page) {
                    throw new IllegalArgumentException("No such ID, end of heap reached");
                }
                return;
            }
        }
    }

    private int[] getPage(int page) {
        int cslot = page % cachePageId.length;
        int cp = cachePageId[cslot];
        if (cp == page) {
            return cachePageData[cslot];
        }
        else {
            cachePageId[cslot] = page;
            readPage((long)(page ) * pageSize, offsetMap[page], cachePageData[cslot]);
            return cachePageData[cslot];
        }
    }

    static void scan(HprofHeap heap) {
        long[] pointer = new long[1];
        TagBounds bounds = heap.getAllInstanceDumpBounds();
        pointer[0] = bounds.startOffset;

        while(pointer[0] < bounds.endOffset) {
            long ptr = pointer[0];
            int tag = heap.readDumpTag(pointer);

            if (   tag == HprofHeap.INSTANCE_DUMP
                || tag == HprofHeap.OBJECT_ARRAY_DUMP
                || tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                long iid = heap.dumpBuffer.getID(ptr + 1);
                System.out.println(ptr + " - " + iid);
            }
        }
    }

    private void readPage(long id, long offset, int[] cachePage) {
        TagBounds bounds = heap.getAllInstanceDumpBounds();
        pointer[0] = offset;
        Arrays.fill(cachePage, -1);

        while(pointer[0] < bounds.endOffset) {
            long ptr = pointer[0];
            int tag = heap.readDumpTag(pointer);

            if (   tag == HprofHeap.INSTANCE_DUMP
                || tag == HprofHeap.OBJECT_ARRAY_DUMP
                || tag == HprofHeap.PRIMITIVE_ARRAY_DUMP
                || tag == HprofHeap.CLASS_DUMP) {
                long iid = compressID(dumpBuffer.getID(ptr + 1));
                long rel = iid - id;
                if (rel >= pageSize) {
                    // pointer to first object on next page
                    pointer[0] = ptr;
                    break;
                }
                long shift = ptr - offset;
                if (shift > Integer.MAX_VALUE) {
                    throw new RuntimeException("Address gap limit exceeded: " + shift);
                }
                cachePage[(int) rel] = (int) shift;
            }
        }
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

    private long compressID(long origId) {
        if (idOffset > origId) {
            throw new IllegalArgumentException("ID is below threshold (" + idOffset + "): " + origId);
        }
        if ((origId - idOffset) % allignment != 0) {
            throw new IllegalArgumentException("ID is not alligned: " + origId);
        }
        return (origId - idOffset) / allignment;
    }
}
