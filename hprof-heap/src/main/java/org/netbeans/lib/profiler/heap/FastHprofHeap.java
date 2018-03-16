/**
 * Copyright 2014 Alexey Ragozin
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.jvmtool.heapdump.MissingInstance;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class FastHprofHeap extends HprofHeap {

    public static HprofByteBuffer newFileBuffer(File file) throws IOException {
        return HeapFactory.createHprofByteBuffer(file, HeapFactory.DEFAULT_BUFFER);
    }

    private Map<Long, ClassEntry> classes;
    private HeapOffsetMap offsetMap;
    private boolean missingStubsEnabled;

    /**
     * Please use {@link HeapFactory}
     */
    protected FastHprofHeap(File dumpFile, int seg) throws FileNotFoundException, IOException {
        super(dumpFile, seg);
        classes = new HashMap<Long, ClassEntry>();
        offsetMap = new HeapOffsetMap(this);
    }

    /**
     * Please use {@link HeapFactory}
     */
    protected FastHprofHeap(HprofByteBuffer dumpBuffer, int seg) throws FileNotFoundException, IOException {
        super(dumpBuffer, seg);
        classes = new HashMap<Long, ClassEntry>();
        offsetMap = new HeapOffsetMap(this);
    }

    public void enableMissingInstanceStubs(boolean enabled) {
    	this.missingStubsEnabled = enabled;
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
        try {
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

			    if (classDump == null) {
			        throw new IllegalInstanceIDException("Missing instance type (" + classId + ") ID: " + instanceID);
			    }
			}

			
			if (tag == INSTANCE_DUMP) {
			    return new InstanceDump(classDump, start);
			} else if (tag == OBJECT_ARRAY_DUMP) {
			    return new ObjectArrayDump(classDump, start);
			} else if (tag == CLASS_DUMP) {
			    return new ClassDumpInstance(classDump);
			} else {
			    throw new IllegalInstanceIDException("Illegal tag " + tag + " ID: " + instanceID); // NOI18N
			}
		} catch (IllegalInstanceIDException e) {
			if (missingStubsEnabled) {
				return new MissingInstance(instanceID);
			}
			else {
				throw e;
			}
		}
    }

    @Override
    public JavaClass getJavaClassByID(long javaclassId) {
        List<JavaClass> jc = getClassDumpSegment().createClassCollection();
        ClassEntry ce = classes.get(javaclassId);
        if (ce != null) {
            return jc.get(ce.index - 1);
        }
        else {
            return null;
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
