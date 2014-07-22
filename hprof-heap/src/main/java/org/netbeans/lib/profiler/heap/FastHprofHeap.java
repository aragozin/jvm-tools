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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class FastHprofHeap extends HprofHeap {

    private Map<Long, ClassEntry> classes;
    private HeapOffsetMap offsetMap;

    FastHprofHeap(File dumpFile, int seg) throws FileNotFoundException, IOException {
        super(dumpFile, seg);
        classes = new HashMap<Long, ClassEntry>();
        offsetMap = new HeapOffsetMap(this);
    }

    @Override
    protected LongMap initIdMap() throws FileNotFoundException, IOException {
        return null;
    }

    @Override
    boolean eagerInstanceCounting() {
        return false;
    }

    @Override
    int idToInstanceNumber(long instanceId) {
        ClassEntry ce = classes.get(instanceId);
        if (ce != null) {
            return ce.index;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    long idToDumpOffset(long instanceId) {
        ClassEntry ce = classes.get(instanceId);
        if (ce != null) {
            return ce.offset;
        }
        else {
            return offsetMap.offset(instanceId);
        }
    }



    @Override
    void addClassEntry(long instanceId, long offset, int index) {
        ClassEntry ce = new ClassEntry();
        ce.offset = offset;
        ce.index = index;
        classes.put(instanceId, ce);
    }

    public Instance getInstanceByID(long instanceID) {
        if (instanceID == 0L) {
            return null;
        }

        computeInstances();

        ClassDump classDump;
        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        int idSize = dumpBuffer.getIDSize();
        int classIdOffset = 0;

        long start = idToDumpOffset(instanceID);
        assert start != 0L;

        long[] offset = new long[] { start };

        int tag = readDumpTag(offset);

        if (tag == INSTANCE_DUMP) {
            classIdOffset = idSize + 4;
        } else if (tag == OBJECT_ARRAY_DUMP) {
            classIdOffset = idSize + 4 + 4;
        } else if (tag == PRIMITIVE_ARRAY_DUMP) {
            classIdOffset = idSize + 4 + 4;
        }

        if (tag == PRIMITIVE_ARRAY_DUMP) {
            classDump = classDumpBounds.getPrimitiveArrayClass(dumpBuffer.get(start + 1 + classIdOffset));

            return new PrimitiveArrayDump(classDump, start);
        } else {
            long classId = dumpBuffer.getID(start + 1 + classIdOffset);
            classDump = classDumpBounds.getClassDumpByID(classId);
        }

        if (tag == INSTANCE_DUMP) {
            return new InstanceDump(classDump, start);
        } else if (tag == OBJECT_ARRAY_DUMP) {
            return new ObjectArrayDump(classDump, start);
        } else if (tag == CLASS_DUMP) {
            return new ClassDumpInstance(classDump);
        } else {
            throw new IllegalArgumentException("Illegal tag " + tag); // NOI18N
        }
    }

    @Override
    public List<Instance> getBiggestObjectsByRetainedSize(int number) {
        throw new UnsupportedOperationException();
    }

    @Override
    long getRetainedSize(Instance instance) {
        throw new UnsupportedOperationException();
    }

    @Override
    synchronized void computeInstances() {
        // do nothing
    }

    @Override
    synchronized void computeReferences() {
        throw new UnsupportedOperationException();
    }

    @Override
    synchronized void computeRetainedSize() {
        throw new UnsupportedOperationException();
    }

    private static class ClassEntry {
        long offset;
        int index;
    }
}
