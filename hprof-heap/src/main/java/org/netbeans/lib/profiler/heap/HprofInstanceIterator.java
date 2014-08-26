/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.util.NoSuchElementException;

class HprofInstanceIterator implements Iterator<Instance> {

    private final HprofHeap heap;
    private final TagBounds allInstanceDumpBounds;
    private final long[] pointer;
    private final HprofByteBuffer dumpBuffer;
    private Instance nextInstance;


    public HprofInstanceIterator(HprofHeap heap, long start) {
        this.heap = heap;
        allInstanceDumpBounds = heap.getAllInstanceDumpBounds();
        pointer = new long[]{start};
        dumpBuffer = heap.dumpBuffer;
        nextInstance = seek();
    }

    @Override
    public boolean hasNext() {
        return nextInstance != null;
    }

    @Override
    public Instance next() {
        if (nextInstance == null) {
            throw new NoSuchElementException();
        }
        else {
            Instance i = nextInstance;
            nextInstance = seek();
            return i;
        }
    }

    Instance seek() {
        while(pointer[0] < allInstanceDumpBounds.endOffset) {

            long start = pointer[0];
            int classIdOffset = 0;
            long instanceClassId = 0L;
            int tag = heap.readDumpTag(pointer);
            int idSize = dumpBuffer.getIDSize();

            if (tag == HprofHeap.INSTANCE_DUMP) {
                classIdOffset = idSize + 4;
            } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
                classIdOffset = idSize + 4 + 4;
            } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                byte type = dumpBuffer.get(start + 1 + idSize + 4 + 4);
                instanceClassId = heap.getClassDumpSegment().getPrimitiveArrayClass(type).getJavaClassId();
            }

            if (classIdOffset != 0) {
                instanceClassId = dumpBuffer.getID(start + 1 + classIdOffset);
            }

            ClassDump jc = (ClassDump) heap.getJavaClassByID(instanceClassId);

            Instance instance = null;
            if (tag == HprofHeap.INSTANCE_DUMP) {
                instance = new InstanceDump(jc, start);
            } else if (tag == HprofHeap.OBJECT_ARRAY_DUMP) {
                instance = new ObjectArrayDump(jc, start);
            } else if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                instance = new PrimitiveArrayDump(jc, start);
            } else {
                // ignore
                continue;
            }

            return instance;
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    static class AsIterable implements Iterable<Instance> {

        private final HprofHeap heap;
        private final long start;

        public AsIterable(HprofHeap heap) {
            this.heap = heap;
            this.start = heap.getAllInstanceDumpBounds().startOffset;
        }

        public AsIterable(HprofHeap heap, long start) {
            this.heap = heap;
            this.start = start;
        }

        @Override
        public Iterator<Instance> iterator() {
            return new HprofInstanceIterator(heap, start);
        }
    }
}
