/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Tomas Hurka
 */
class HprofHeap implements Heap {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // dump tags
    static final int STRING = 1;
    static final int LOAD_CLASS = 2;
    private static final int UNLOAD_CLASS = 3;
    static final int STACK_FRAME = 4;
    static final int STACK_TRACE = 5;
    private static final int ALLOC_SITES = 6;
    static final int HEAP_SUMMARY = 7;
    private static final int START_THREAD = 0xa;
    private static final int END_THREAD = 0xb;
    private static final int HEAP_DUMP = 0xc;
    private static final int HEAP_DUMP_SEGMENT = 0x1c;
    private static final int HEAP_DUMP_END = 0x2c;
    private static final int CPU_SAMPLES = 0xd;
    private static final int CONTROL_SETTINGS = 0xe;

    // heap dump tags
    static final int ROOT_UNKNOWN = 0xff;
    static final int ROOT_JNI_GLOBAL = 1;
    static final int ROOT_JNI_LOCAL = 2;
    static final int ROOT_JAVA_FRAME = 3;
    static final int ROOT_NATIVE_STACK = 4;
    static final int ROOT_STICKY_CLASS = 5;
    static final int ROOT_THREAD_BLOCK = 6;
    static final int ROOT_MONITOR_USED = 7;
    static final int ROOT_THREAD_OBJECT = 8;
    static final int CLASS_DUMP = 0x20;
    static final int INSTANCE_DUMP = 0x21;
    static final int OBJECT_ARRAY_DUMP = 0x22;
    static final int PRIMITIVE_ARRAY_DUMP = 0x23;

    // basic type
    static final int OBJECT = 2;
    static final int BOOLEAN = 4;
    static final int CHAR = 5;
    static final int FLOAT = 6;
    static final int DOUBLE = 7;
    static final int BYTE = 8;
    static final int SHORT = 9;
    static final int INT = 10;
    static final int LONG = 11;
    private static final boolean DEBUG = false;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofByteBuffer dumpBuffer;
    LongMap idToOffsetMap;
    private NearestGCRoot nearestGCRoot;
    private ComputedSummary computedSummary;
    private Map gcRoots;
    private DominatorTree domTree;
    final private Object gcRootLock = new Object();
    private TagBounds allInstanceDumpBounds;
    private TagBounds heapDumpSegment;
    private TagBounds[] heapTagBounds;
    private TagBounds[] tagBounds = new TagBounds[0xff];
    private boolean instancesCountComputed;
    private boolean referencesComputed;
    private boolean retainedSizeComputed;
    private boolean retainedSizeByClassComputed;
    private int idMapSize;
    private int segment;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofHeap(File dumpFile, int seg) throws FileNotFoundException, IOException {
        dumpBuffer = HprofByteBuffer.createHprofByteBuffer(dumpFile);
        segment = seg;
        fillTagBounds(dumpBuffer.getHeaderSize());
        heapDumpSegment = computeHeapDumpStart();

        if (heapDumpSegment != null) {
            fillHeapTagBounds();
        }

        idToOffsetMap = new LongMap(idMapSize,dumpBuffer.getIDSize(),dumpBuffer.getFoffsetSize());
        nearestGCRoot = new NearestGCRoot(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public List /*<JavaClass>*/ getAllClasses() {
        ClassDumpSegment classDumpBounds;

        if (heapDumpSegment == null) {
            return Collections.EMPTY_LIST;
        }

        classDumpBounds = getClassDumpSegment();

        if (classDumpBounds == null) {
            return Collections.EMPTY_LIST;
        }

        return classDumpBounds.createClassCollection();
    }

    public List getBiggestObjectsByRetainedSize(int number) {
        long[] ids;
        List bigObjects = new ArrayList(number);

        computeRetainedSize();
        ids = idToOffsetMap.getBiggestObjectsByRetainedSize(number);
        for (int i=0;i<ids.length;i++) {
            bigObjects.add(getInstanceByID(ids[i]));
        }
        return bigObjects;
    }

    public GCRoot getGCRoot(Instance instance) {
       Long instanceId = Long.valueOf(instance.getInstanceId());
       return getGCRoot(instanceId);
    }

    public Collection getGCRoots() {
        synchronized (gcRootLock) {
            if (heapDumpSegment == null) {
                return Collections.EMPTY_LIST;
            }
            if (gcRoots == null) {
                gcRoots = computeGCRootsFor(heapTagBounds[ROOT_UNKNOWN]);
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_JNI_GLOBAL]));
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_JNI_LOCAL]));
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_JAVA_FRAME]));
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_NATIVE_STACK]));
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_STICKY_CLASS]));
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_THREAD_BLOCK]));
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_MONITOR_USED]));
                gcRoots.putAll(computeGCRootsFor(heapTagBounds[ROOT_THREAD_OBJECT]));
            }

            return gcRoots.values();
        }
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
        LongMap.Entry entry = idToOffsetMap.get(instanceID);

        if (entry == null) {
            return null;
        }

        long start = entry.getOffset();
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

    public JavaClass getJavaClassByID(long javaclassId) {
        return getClassDumpSegment().getClassDumpByID(javaclassId);
    }

    public JavaClass getJavaClassByName(String fqn) {
        if (heapDumpSegment == null) {
            return null;
        }
        return getClassDumpSegment().getJavaClassByName(fqn);
    }

    public Collection getJavaClassesByRegExp(String regexp) {
        if (heapDumpSegment == null) {
            return Collections.EMPTY_LIST;
        }
        return getClassDumpSegment().getJavaClassesByRegExp(regexp);
    }

    public synchronized HeapSummary getSummary() {
        TagBounds summaryBound = tagBounds[HEAP_SUMMARY];

        if (summaryBound != null) {
            return new Summary(dumpBuffer, summaryBound.startOffset);
        }

        if (computedSummary == null) {
            computedSummary = new ComputedSummary(this);
        }

        return computedSummary;
    }

    public Properties getSystemProperties() {
        JavaClass systemClass = getJavaClassByName("java.lang.System"); // NOI18N
        if (systemClass != null) {
            Instance props = (Instance) systemClass.getValueOfStaticField("props"); //NOI18N

            if (props == null) {
                props = (Instance) systemClass.getValueOfStaticField("systemProperties"); //NOI18N
            }
            if (props != null) {
                return HprofProxy.getProperties(props);
            }
        }
        return null;
    }

    ClassDumpSegment getClassDumpSegment() {
        return (ClassDumpSegment) heapTagBounds[CLASS_DUMP];
    }

    LoadClassSegment getLoadClassSegment() {
        return (LoadClassSegment) tagBounds[LOAD_CLASS];
    }

    StringSegment getStringSegment() {
        return (StringSegment) tagBounds[STRING];
    }

    StackTraceSegment getStackTraceSegment() {
        return (StackTraceSegment) tagBounds[STACK_TRACE];
    }

    StackFrameSegment getStackFrameSegment() {
        return (StackFrameSegment) tagBounds[STACK_FRAME];
    }

    TagBounds getAllInstanceDumpBounds() {
        return allInstanceDumpBounds;
    }

    long getRetainedSize(Instance instance) {
        computeRetainedSize();
        return idToOffsetMap.get(instance.getInstanceId()).getRetainedSize();
    }

    GCRoot getGCRoot(Long instanceId) {
        synchronized (gcRootLock) {
            if (gcRoots == null) {
                getGCRoots();
            }

            return (GCRoot) gcRoots.get(instanceId);
        }
    }

    int getValueSize(final byte type) {
        switch (type) {
            case HprofHeap.OBJECT:
                return dumpBuffer.getIDSize();
            case HprofHeap.BOOLEAN:
                return 1;
            case HprofHeap.CHAR:
                return 2;
            case HprofHeap.FLOAT:
                return 4;
            case HprofHeap.DOUBLE:
                return 8;
            case HprofHeap.BYTE:
                return 1;
            case HprofHeap.SHORT:
                return 2;
            case HprofHeap.INT:
                return 4;
            case HprofHeap.LONG:
                return 8;
            default:
                throw new IllegalArgumentException("Invalid type " + type); // NOI18N
        }
    }

    synchronized void computeInstances() {
        if (instancesCountComputed) {
            return;
        }

        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        int idSize = dumpBuffer.getIDSize();
        long[] offset = new long[] { allInstanceDumpBounds.startOffset };
        Map classIdToClassMap = classDumpBounds.getClassIdToClassMap();

        for (long counter = 0; offset[0] < allInstanceDumpBounds.endOffset; counter++) {
            int classIdOffset = 0;
            int instanceIdOffset = 0;
            ClassDump classDump = null;
            long start = offset[0];
            int tag = readDumpTag(offset);
            LongMap.Entry instanceEntry = null;

            if (tag == INSTANCE_DUMP) {
                instanceIdOffset = 1;
                classIdOffset = idSize + 4;
            } else if (tag == OBJECT_ARRAY_DUMP) {
                instanceIdOffset = 1;
                classIdOffset = idSize + 4 + 4;
            } else if (tag == PRIMITIVE_ARRAY_DUMP) {
                byte type = dumpBuffer.get(start + 1 + idSize + 4 + 4);
                instanceIdOffset = 1;
                classDump = classDumpBounds.getPrimitiveArrayClass(type);
            }

            if (instanceIdOffset != 0) {
                long instanceId = dumpBuffer.getID(start + instanceIdOffset);
                instanceEntry = idToOffsetMap.put(instanceId, start);
            }

            if (classIdOffset != 0) {
                long classId = dumpBuffer.getID(start + 1 + classIdOffset);
                classDump = (ClassDump) classIdToClassMap.get(new Long(classId));
            }

            if (classDump != null) {
                classDump.registerInstance(start);
                instanceEntry.setIndex(classDump.getInstancesCount());
                classDumpBounds.addInstanceSize(classDump, tag, start);
            }
            HeapProgress.progress(counter,allInstanceDumpBounds.startOffset,start,allInstanceDumpBounds.endOffset);
        }
        HeapProgress.progressFinish();
        instancesCountComputed = true;
    }

    List findReferencesFor(long instanceId) {
        assert instanceId != 0L : "InstanceID is null";
        computeReferences();

        List refs = new ArrayList();
        List refIds = idToOffsetMap.get(instanceId).getReferences();
        Iterator refIdsIt = refIds.iterator();
        int idSize = dumpBuffer.getIDSize();
        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        long[] offset = new long[1];

        while (refIdsIt.hasNext()) {
            long foundInstanceId = ((Long)refIdsIt.next()).longValue();
            offset[0] = idToOffsetMap.get(foundInstanceId).getOffset();
            int classIdOffset = 0;
            int instanceIdOffset = 0;
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP) {
                classIdOffset = idSize + 4;
                int size = dumpBuffer.getInt(start + 1 + idSize + 4 + idSize);
                byte[] fields = new byte[size];
                dumpBuffer.get(start + 1 + idSize + 4 + idSize + 4, fields);
                long classId = dumpBuffer.getID(start + 1 + idSize + 4);
                ClassDump classDump = classDumpBounds.getClassDumpByID(classId);
                InstanceDump instance = new InstanceDump(classDump, start);
                Iterator fieldIt = instance.getFieldValues().iterator();

                while (fieldIt.hasNext()) {
                    Object field = fieldIt.next();

                    if (field instanceof HprofInstanceObjectValue) {
                        HprofInstanceObjectValue objectValue = (HprofInstanceObjectValue) field;

                        if (objectValue.getInstanceId() == instanceId) {
                            refs.add(objectValue);
                        }
                    }
                }
                if (refs.isEmpty() && classId == instanceId) {
                    SyntheticClassField syntheticClassField = new SyntheticClassField(classDump);
                    long fieldOffset = start + 1 + dumpBuffer.getIDSize() + 4;

                    refs.add(new SyntheticClassObjectValue(instance,syntheticClassField,fieldOffset));
                }
            } else if (tag == OBJECT_ARRAY_DUMP) {
                int elements = dumpBuffer.getInt(start + 1 + idSize + 4);
                int i;
                long position = start + 1 + idSize + 4 + 4 + idSize;

                for (i = 0; i < elements; i++, position += idSize) {
                    if (dumpBuffer.getID(position) == instanceId) {
                        long classId = dumpBuffer.getID(start + 1 + idSize + 4 + 4);
                        ClassDump classDump = classDumpBounds.getClassDumpByID(classId);

                        refs.add(new HprofArrayValue(classDump, start, i));
                    }
                }
            } else if (tag == CLASS_DUMP) {
                ClassDump cls = classDumpBounds.getClassDumpByID(foundInstanceId);
                cls.findStaticReferencesFor(instanceId, refs);
            }
        }

        return refs;
    }

    synchronized void computeReferences() {
        if (referencesComputed) {
            return;
        }

        ClassDumpSegment classDumpBounds = getClassDumpSegment();
        int idSize = dumpBuffer.getIDSize();
        long[] offset = new long[] { allInstanceDumpBounds.startOffset };
        Map classIdToClassMap = classDumpBounds.getClassIdToClassMap();

        computeInstances();
        for (long counter=0; offset[0] < allInstanceDumpBounds.endOffset; counter++) {
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP) {
                long classId = dumpBuffer.getID(start+1+idSize+4);
                ClassDump classDump = (ClassDump) classIdToClassMap.get(new Long(classId));
                long instanceId = dumpBuffer.getID(start+1);
                long inOff = start+1+idSize+4+idSize+4;
                List fields = classDump.getAllInstanceFields();
                Iterator fit = fields.iterator();

                while(fit.hasNext()) {
                    HprofField field = (HprofField) fit.next();
                    if (field.getValueType() == HprofHeap.OBJECT) {
                        long outId = dumpBuffer.getID(inOff);

                        if (outId != 0) {
                            LongMap.Entry entry = idToOffsetMap.get(outId);
                            if (entry != null) {
                                entry.addReference(instanceId);
                            } else {
                                //    System.err.println("instance entry:" + Long.toHexString(outId));
                            }
                        }
                    }
                    inOff += field.getValueSize();
                }
            } else if (tag == OBJECT_ARRAY_DUMP) {
                long instanceId = dumpBuffer.getID(start+1);
                int elements = dumpBuffer.getInt(start+1+idSize+4);
                long position = start+1+idSize+4+4+idSize;

                for(int i=0;i<elements;i++,position+=idSize) {
                    long outId = dumpBuffer.getID(position);

                    if (outId == 0) continue;
                    LongMap.Entry entry = idToOffsetMap.get(outId);
                    if (entry != null) {
                        entry.addReference(instanceId);
                    } else {
                        //    System.err.println("bad array entry:" + Long.toHexString(outId));
                    }
                }
            }
            HeapProgress.progress(counter,allInstanceDumpBounds.startOffset,start,allInstanceDumpBounds.endOffset);
        }

        Iterator classesIt = getClassDumpSegment().createClassCollection().iterator();

        while (classesIt.hasNext()) {
            ClassDump classDump = (ClassDump)classesIt.next();
            List fields = classDump.getStaticFieldValues();
            Iterator fit = fields.iterator();

            while(fit.hasNext()) {
                Object field = fit.next();
                if (field instanceof HprofFieldObjectValue) {
                    long outId = ((HprofFieldObjectValue)field).getInstanceID();

                    if (outId != 0) {
                        LongMap.Entry entry = idToOffsetMap.get(outId);
                        if (entry == null) {
                            //    System.err.println("instance entry:" + Long.toHexString(outId));
                            continue;
                        }
                        entry.addReference(classDump.getJavaClassId());
                    }
                }
            }
        }
        idToOffsetMap.flush();
        HeapProgress.progressFinish();
        referencesComputed = true;
    }

    synchronized void computeRetainedSize() {
        if (retainedSizeComputed) {
            return;
        }
        new TreeObject(this,nearestGCRoot.getLeaves()).computeTrees();
        domTree = new DominatorTree(this,nearestGCRoot.getMultipleParents());
        domTree.computeDominators();
        long[] offset = new long[] { allInstanceDumpBounds.startOffset };

        while (offset[0] < allInstanceDumpBounds.endOffset) {
            int instanceIdOffset = 0;
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP) {
                instanceIdOffset = 1;
            } else if (tag == OBJECT_ARRAY_DUMP) {
                instanceIdOffset = 1;
            } else if (tag == PRIMITIVE_ARRAY_DUMP) {
                instanceIdOffset = 1;
            } else {
                continue;
            }
            long instanceId = dumpBuffer.getID(start + instanceIdOffset);
            LongMap.Entry instanceEntry = idToOffsetMap.get(instanceId);
            long idom = domTree.getIdomId(instanceId,instanceEntry);
            boolean isTreeObj = instanceEntry.isTreeObj();
            long instSize = 0;

            if (!isTreeObj && (instanceEntry.getNearestGCRootPointer() != 0 || getGCRoot(new Long(instanceId)) != null)) {
                long origSize = instanceEntry.getRetainedSize();
                if (origSize < 0) origSize = 0;
                instSize = getInstanceByID(instanceId).getSize();
                instanceEntry.setRetainedSize(origSize + instSize);
            }
            if (idom != 0) {
                long size;
                LongMap.Entry entry;

                if (isTreeObj) {
                    size = instanceEntry.getRetainedSize();
                } else {
                    assert instSize != 0;
                    size = instSize;
                }
                for (;idom!=0;idom=domTree.getIdomId(idom,entry)) {
                    entry = idToOffsetMap.get(idom);
                    if (entry.isTreeObj()) {
                        break;
                    }
                    long retainedSize = entry.getRetainedSize();
                    if (retainedSize < 0) retainedSize = 0;
                    entry.setRetainedSize(retainedSize+size);
                }
            }
        }
        retainedSizeComputed = true;
    }

    synchronized void computeRetainedSizeByClass() {
        if (retainedSizeByClassComputed) {
            return;
        }
        computeRetainedSize();
        long[] offset = new long[] { allInstanceDumpBounds.startOffset };

        while (offset[0] < allInstanceDumpBounds.endOffset) {
            int instanceIdOffset = 0;
            long start = offset[0];
            int tag = readDumpTag(offset);

            if (tag == INSTANCE_DUMP) {
                instanceIdOffset = 1;
            } else if (tag == OBJECT_ARRAY_DUMP) {
                instanceIdOffset = 1;
            } else if (tag == PRIMITIVE_ARRAY_DUMP) {
                instanceIdOffset = 1;
            } else {
                continue;
            }
            long instanceId = dumpBuffer.getID(start + instanceIdOffset);
            Instance i = getInstanceByID(instanceId);
            ClassDump javaClass = (ClassDump) i.getJavaClass();
            if (javaClass != null && !domTree.hasInstanceInChain(tag, i)) {
                javaClass.addSizeForInstance(i);
            }
        }
        // all done, release domTree
        domTree = null;
        retainedSizeByClassComputed = true;
    }

    synchronized Instance getNearestGCRootPointer(Instance instance) {
        return nearestGCRoot.getNearestGCRootPointer(instance);
    }

    int readDumpTag(long[] offset) {
        long position = offset[0];
        int dumpTag = dumpBuffer.get(position++);
        long size = 0;
        long tagOffset = position;
        int idSize = dumpBuffer.getIDSize();

        switch (dumpTag) {
            case -1:
            case ROOT_UNKNOWN:

                if (DEBUG) {
                    System.out.println("Tag ROOT_UNKNOWN"); // NOI18N
                }

                size = idSize;
                dumpTag = ROOT_UNKNOWN;

                break;
            case ROOT_JNI_GLOBAL:

                if (DEBUG) {
                    System.out.println("Tag ROOT_JNI_GLOBAL"); // NOI18N
                }

                size = 2 * idSize;

                break;
            case ROOT_JNI_LOCAL: {
                if (DEBUG) {
                    System.out.print("Tag ROOT_JNI_LOCAL"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int threadSerial = dumpBuffer.getInt(position);
                    position += 4;

                    int frameNum = dumpBuffer.getInt(position);
                    position += 4;
                    System.out.println(" Object ID " + objId + " Thread serial " + threadSerial + " Frame num " + frameNum); // NOI18N
                }

                size = idSize + (2 * 4);

                break;
            }
            case ROOT_JAVA_FRAME:

                if (DEBUG) {
                    System.out.println("Tag ROOT_JAVA_FRAME"); // NOI18N
                    int threadSerial = dumpBuffer.getInt(position);
                    position += 4;

                    int frameNum = dumpBuffer.getInt(position);
                    position += 4;
                    System.out.println(" Thread serial " + threadSerial + " Frame num " + frameNum); // NOI18N
                }

                size = idSize + (2 * 4);

                break;
            case ROOT_NATIVE_STACK:

                if (DEBUG) {
                    System.out.println("Tag ROOT_NATIVE_STACK"); // NOI18N
                }

                size = idSize + 4;

                break;
            case ROOT_STICKY_CLASS:

                if (DEBUG) {
                    System.out.println("Tag ROOT_STICKY_CLASS"); // NOI18N
                }

                size = idSize;

                break;
            case ROOT_THREAD_BLOCK:

                if (DEBUG) {
                    System.out.println("Tag ROOT_THREAD_BLOCK"); // NOI18N
                }

                size = idSize + 4;

                break;
            case ROOT_MONITOR_USED:

                if (DEBUG) {
                    System.out.println("Tag ROOT_MONITOR_USED"); // NOI18N
                }

                size = idSize;

                break;
            case ROOT_THREAD_OBJECT:

                if (DEBUG) {
                    System.out.println("Tag ROOT_THREAD_OBJECT"); // NOI18N
                }

                size = idSize + (2 * 4);

                break;
            case CLASS_DUMP: {
                int constantSize = idSize + 4 + (6 * idSize) + 4;
                int cpoolSize;
                int sfSize;
                int ifSize;

                if (DEBUG) {
                    System.out.println("Tag CLASS_DUMP, start offset " + tagOffset); // NOI18N

                    long classId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;

                    long superId = dumpBuffer.getID(position);
                    position += idSize;

                    long classLoaderId = dumpBuffer.getID(position);
                    position += idSize;

                    long signersId = dumpBuffer.getID(position);
                    position += idSize;

                    long protDomainId = dumpBuffer.getID(position);
                    position += idSize;
                    dumpBuffer.getID(position);
                    position += idSize;
                    dumpBuffer.getID(position);
                    position += idSize;

                    int instSize = dumpBuffer.getInt(position);
                    position += 4;
                    offset[0] = position;
                    cpoolSize = readConstantPool(offset);
                    sfSize = readStaticFields(offset);
                    ifSize = readInstanceFields(offset);
                    System.out.println("ClassId " + classId + " stack Serial " + stackSerial + " Super ID " + superId       // NOI18N
                                       + " ClassLoader ID " + classLoaderId + " signers " + signersId + " Protect Dom Id "  // NOI18N
                                       + protDomainId + " Size " + instSize);                                               // NOI18N
                    System.out.println(" Cpool " + cpoolSize + " Static fields " + sfSize + " Instance fileds " + ifSize);  // NOI18N
                } else {
                    offset[0] = position + constantSize;
                    cpoolSize = readConstantPool(offset);
                    sfSize = readStaticFields(offset);
                    ifSize = readInstanceFields(offset);
                }
                size = constantSize + cpoolSize + sfSize + ifSize;

                break;
            }
            case INSTANCE_DUMP: {
                int fieldSize;

                if (DEBUG) {
                    System.out.println("Tag INSTANCE_DUMP"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;

                    long classId = dumpBuffer.getID(position);
                    position += idSize;
                    fieldSize = dumpBuffer.getInt(position);
                    position += 4;
                    System.out.println("Obj ID " + objId + " Stack serial " + stackSerial + " Class ID " + classId
                                       + " Field size " + fieldSize); // NOI18N
                } else {
                    fieldSize = dumpBuffer.getInt(position + idSize + 4 + idSize);
                }

                size = idSize + 4 + idSize + 4 + fieldSize;

                break;
            }
            case OBJECT_ARRAY_DUMP: {
                long elements;

                if (DEBUG) {
                    System.out.println("Tag OBJECT_ARRAY_DUMP"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;
                    elements = dumpBuffer.getInt(position);
                    position += 4;

                    long classId = dumpBuffer.getID(position);
                    position += idSize;

                    int dataSize = 0;

                    System.out.println("Obj ID " + objId + " Stack serial " + stackSerial + " Elements " + elements // NOI18N
                                           + " Type " + classId); // NOI18N

                    for (int i = 0; i < elements; i++) {
                        dataSize += dumpBuffer.getIDSize();
                        System.out.println("Instance ID " + dumpBuffer.getID(position)); // NOI18N
                        position += idSize;
                    }
                } else {
                    elements = dumpBuffer.getInt(position + idSize + 4);
                }

                size = idSize + 4 + 4 + idSize + (elements * idSize);

                break;
            }
            case PRIMITIVE_ARRAY_DUMP: {
                long elements;
                byte type;

                if (DEBUG) {
                    System.out.println("Tag PRIMITINE_ARRAY_DUMP"); // NOI18N

                    long objId = dumpBuffer.getID(position);
                    position += idSize;

                    int stackSerial = dumpBuffer.getInt(position);
                    position += 4;
                    elements = dumpBuffer.getInt(position);
                    position += 4;
                    type = dumpBuffer.get(position++);

                    int dataSize = 0;
                    System.out.println("Obj ID " + objId + " Stack serial " + stackSerial + " Elements " + elements + " Type " + type); // NOI18N

                    for (int i = 0; i < elements; i++) {
                        dataSize += getValueSize(type);
                    }
                } else {
                    elements = dumpBuffer.getInt(position + idSize + 4);
                    type = dumpBuffer.get(position + idSize + 4 + 4);
                }

                size = idSize + 4 + 4 + 1 + (elements * getValueSize(type));

                break;
            }
            case HEAP_DUMP_SEGMENT: { // to handle big dumps
                size = 4 + 4;

                break;
            }
            default:throw new IllegalArgumentException("Invalid dump tag " + dumpTag + " at position " + (position - 1)); // NOI18N
        }

        offset[0] = tagOffset + size;

        return dumpTag;
    }

    int readTag(long[] offset) {
        long start = offset[0];
        int tag = dumpBuffer.get(start);

        //int time = dumpBuffer.getInt(start+1);
        long len = dumpBuffer.getInt(start + 1 + 4) & 0xFFFFFFFFL;  // len is unsigned int
        offset[0] = start + 1 + 4 + 4 + len;

        return tag;
    }

    private Map computeGCRootsFor(TagBounds tagBounds) {
        Map roots = new HashMap();

        if (tagBounds != null) {
            int rootTag = tagBounds.tag;
            long[] offset = new long[] { tagBounds.startOffset };

            while (offset[0] < tagBounds.endOffset) {
                long start = offset[0];

                if (readDumpTag(offset) == rootTag) {
                    HprofGCRoot root;
                    if (rootTag == ROOT_THREAD_OBJECT) {
                        root = new ThreadObjectHprofGCRoot(this, start);
                    } else if (rootTag == ROOT_JAVA_FRAME) {
                        root = new JavaFrameHprofGCRoot(this, start);
                    } else {
                        root = new HprofGCRoot(this, start);
                    }
                    roots.put(Long.valueOf(root.getInstanceId()), root);
                }
            }
        }

        return roots;
    }

    private TagBounds computeHeapDumpStart() throws IOException {
        TagBounds heapDumpBounds = tagBounds[HEAP_DUMP];

        if (heapDumpBounds != null) {
            long start = heapDumpBounds.startOffset;
            long[] offset = new long[] { start };

            for (int i = 0; (i <= segment) && (start < heapDumpBounds.endOffset);) {
                int tag = readTag(offset);

                if (tag == HEAP_DUMP) {
                    if (i == segment) {
                        return new TagBounds(HEAP_DUMP, start, offset[0]);
                    } else {
                        i++;
                    }
                }

                start = offset[0];
            }

            throw new IOException("Invalid segment " + segment); // NOI18N
        } else {
            TagBounds heapDumpSegmentBounds = tagBounds[HEAP_DUMP_SEGMENT];

            if (heapDumpSegmentBounds != null) {
                long start = heapDumpSegmentBounds.startOffset;
                long end = heapDumpSegmentBounds.endOffset;

                return new TagBounds(HEAP_DUMP, start, end);
            }
        }

        return null;
    }

    private void fillHeapTagBounds() {
        if (heapTagBounds != null) {
            return;
        }

        heapTagBounds = new TagBounds[0x100];

        long[] offset = new long[] { heapDumpSegment.startOffset + 1 + 4 + 4 };

        for (long counter=0; offset[0] < heapDumpSegment.endOffset; counter++) {
            long start = offset[0];
            int tag = readDumpTag(offset);
            TagBounds bounds = heapTagBounds[tag];
            long end = offset[0];

            if (bounds == null) {
                TagBounds newBounds;

                if (tag == CLASS_DUMP) {
                    newBounds = new ClassDumpSegment(this, start, end);
                } else {
                    newBounds = new TagBounds(tag, start, end);
                }

                heapTagBounds[tag] = newBounds;
            } else {
                bounds.endOffset = end;
            }

            if ((tag == CLASS_DUMP) || (tag == INSTANCE_DUMP) || (tag == OBJECT_ARRAY_DUMP) || (tag == PRIMITIVE_ARRAY_DUMP)) {
                idMapSize++;
            }
            HeapProgress.progress(counter,heapDumpSegment.startOffset,start,heapDumpSegment.endOffset);
        }

        TagBounds instanceDumpBounds = heapTagBounds[INSTANCE_DUMP];
        TagBounds objArrayDumpBounds = heapTagBounds[OBJECT_ARRAY_DUMP];
        TagBounds primArrayDumpBounds = heapTagBounds[PRIMITIVE_ARRAY_DUMP];
        allInstanceDumpBounds = instanceDumpBounds.union(objArrayDumpBounds);
        allInstanceDumpBounds = allInstanceDumpBounds.union(primArrayDumpBounds);
        HeapProgress.progressFinish();
    }

    private void fillTagBounds(long tagStart) {
        long[] offset = new long[] { tagStart };

        while (offset[0] < dumpBuffer.capacity()) {
            long start = offset[0];
            int tag = readTag(offset);
            TagBounds bounds = tagBounds[tag];
            long end = offset[0];

            if (bounds == null) {
                TagBounds newBounds;

                if (tag == LOAD_CLASS) {
                    newBounds = new LoadClassSegment(this, start, end);
                } else if (tag == STRING) {
                    newBounds = new StringSegment(this, start, end);
                } else if (tag == STACK_TRACE) {
                    newBounds = new StackTraceSegment(this, start, end);
                } else if (tag == STACK_FRAME) {
                    newBounds = new StackFrameSegment(this, start, end);
                } else {
                    newBounds = new TagBounds(tag, start, end);
                }

                tagBounds[tag] = newBounds;
            } else {
                bounds.endOffset = end;
            }
        }
    }

    private int readConstantPool(long[] offset) {
        long start = offset[0];
        int size = dumpBuffer.getShort(start);
        offset[0] += 2;

        for (int i = 0; i < size; i++) {
            offset[0] += 2;
            readValue(offset);
        }

        return (int) (offset[0] - start);
    }

    private int readInstanceFields(long[] offset) {
        long position = offset[0];
        int fields = dumpBuffer.getShort(offset[0]);
        offset[0] += 2;

        if (DEBUG) {
            for (int i = 0; i < fields; i++) {
                long nameId = dumpBuffer.getID(offset[0]);
                offset[0] += dumpBuffer.getIDSize();

                byte type = dumpBuffer.get(offset[0]++);
                System.out.println("Instance field name ID " + nameId + " Type " + type); // NOI18N
            }
        } else {
            offset[0] += (fields * (dumpBuffer.getIDSize() + 1));
        }

        return (int) (offset[0] - position);
    }

    private int readStaticFields(long[] offset) {
        long start = offset[0];
        int fields = dumpBuffer.getShort(start);
        offset[0] += 2;

        int idSize = dumpBuffer.getIDSize();

        for (int i = 0; i < fields; i++) {
            if (DEBUG) {
                long nameId = dumpBuffer.getID(offset[0]);
                System.out.print("Static field name ID " + nameId + " "); // NOI18N
            }

            offset[0] += idSize;

            byte type = readValue(offset);
        }

        return (int) (offset[0] - start);
    }

    private byte readValue(long[] offset) {
        byte type = dumpBuffer.get(offset[0]++);
        offset[0] += getValueSize(type);

        return type;
    }
}
