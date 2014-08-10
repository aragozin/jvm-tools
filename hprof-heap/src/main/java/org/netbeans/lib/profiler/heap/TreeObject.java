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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Tomas Hurka
 */
class TreeObject {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int BUFFER_SIZE = (64 * 1024) / 8;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HprofHeap heap;
    private LongBuffer readBuffer;
    private LongBuffer writeBuffer;
    private Set<Long> unique;
//private long nextLevelSize;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    TreeObject(HprofHeap h, LongBuffer leaves) {
        heap = h;
        writeBuffer = leaves;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------


    synchronized void computeTrees() {
        boolean changed;
        try {
            createBuffers();
            do {
                switchBuffers();
                changed = computeOneLevel();
//System.out.println("Tree obj.   "+heap.idToOffsetMap.treeObj);
//if (changed) System.out.println("Next level  "+nextLevelSize);
            } while (changed);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        deleteBuffers();
//System.out.println("Done!");
    }

    private boolean computeOneLevel() throws IOException {
//nextLevelSize = 0;
        boolean changed = false;
        for (;;) {
            long instanceId = readLong();
            Instance instance;
            List<FieldValue> fieldValues;
            Iterator<FieldValue> valuesIt;
            long retainedSize = 0;

            if (instanceId == 0) {  // end of level
                break;
            }
            instance = heap.getInstanceByID(instanceId);
            if (instance instanceof ObjectArrayInstance) {
                Iterator<Instance> instanceIt = ((ObjectArrayInstance) instance).getValues().iterator();
                long size = 0;
                while (instanceIt.hasNext() && size != -1) {
                    Instance refInstance = (Instance) instanceIt.next();
                    size = checkInstance(instanceId, refInstance);
                    retainedSize += size;
                }
                changed |= processInstance(instance, size, retainedSize);
                continue;
            } else if (instance instanceof PrimitiveArrayInstance) {
                assert false:"Error - PrimitiveArrayInstance not allowed "+instance.getJavaClass().getName()+"#"+instance.getInstanceNumber();
                continue;
            } else if (instance instanceof ClassDumpInstance) {
                ClassDump javaClass = ((ClassDumpInstance) instance).classDump;

                fieldValues = javaClass.getStaticFieldValues();
            } else if (instance instanceof InstanceDump) {
                fieldValues = instance.getFieldValues();
            } else {
                if (instance == null) {
                    System.err.println("HeapWalker Warning - null instance for " + instanceId); // NOI18N
                    continue;
                }
                throw new IllegalArgumentException("Illegal type " + instance.getClass()); // NOI18N
            }
            long size = 0;
            valuesIt = fieldValues.iterator();
            while (valuesIt.hasNext() && size != -1) {
                FieldValue val = (FieldValue) valuesIt.next();

                if (val instanceof ObjectFieldValue) {
                    Instance refInstance = ((ObjectFieldValue) val).getInstance();
                    size = checkInstance(instanceId,refInstance);
                    retainedSize += size;
                }
            }
            changed |= processInstance(instance, size, retainedSize);
        }
        return changed;
    }

    private boolean processInstance(Instance instance, long size, long retainedSize) throws IOException {
        if (size != -1) {
            LongMap.Entry entry = heap.idToOffsetMap.get(instance.getInstanceId());
            entry.setRetainedSize((int)(instance.getSize()+retainedSize));
            entry.setTreeObj();
            if (entry.hasOnlyOneReference()) {
                long gcRootPointer = entry.getNearestGCRootPointer();
                if (gcRootPointer != 0) {
                    if (!unique.contains(gcRootPointer)) {
                        unique.add(gcRootPointer);
                        writeLong(gcRootPointer);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void createBuffers() {
        readBuffer = new LongBuffer(BUFFER_SIZE);
    }

    private void deleteBuffers() {
        readBuffer.delete();
        writeBuffer.delete();
    }

    private long readLong() throws IOException {
        return readBuffer.readLong();
    }

    private void switchBuffers() throws IOException {
        LongBuffer b = readBuffer;
        readBuffer = writeBuffer;
        writeBuffer = b;
        readBuffer.startReading();
        writeBuffer.reset();
        unique = new HashSet<Long>(4000);
    }

    private void writeLong(long instanceId) throws IOException {
        if (instanceId != 0) {
            writeBuffer.writeLong(instanceId);
//nextLevelSize++;
        }
    }

    private long checkInstance(long instanceId, Instance refInstance) throws IOException {
        if (refInstance != null) {
            LongMap.Entry refEntry = heap.idToOffsetMap.get(refInstance.getInstanceId());

            if (!refEntry.hasOnlyOneReference()) {
                return -1;
            }
            if (!refEntry.isTreeObj()) {
                return -1;
            }
            return refEntry.getRetainedSize();
        }
        return 0;
    }
}
