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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 *
 * @author Tomas Hurka
 */
class ClassDumpSegment extends TagBounds {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofHeap hprofHeap;
    Map /*<JavaClass represeting array,Integer - allInstanceSize>*/ arrayMap;
    final int classIDOffset;
    final int classLoaderIDOffset;
    final int constantPoolSizeOffset;
    final int fieldNameIDOffset;
    final int fieldSize;
    final int fieldTypeOffset;
    final int fieldValueOffset;
    final int instanceSizeOffset;
    final int minimumInstanceSize;
    final int protectionDomainIDOffset;
    final int reserved1;
    final int reserver2;
    final int signersID;
    final int stackTraceSerialNumberOffset;
    final int superClassIDOffset;
    ClassDump java_lang_Class;
    Map /*<JavaClass,List<Field>>*/ fieldsCache;
    private List /*<JavaClass>*/ classes;
    private Map /*<Byte,JavaClass>*/ primitiveArrayMap;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    ClassDumpSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.CLASS_DUMP, start, end);

        int idSize = heap.dumpBuffer.getIDSize();
        hprofHeap = heap;
        // initialize offsets
        classIDOffset = 1;
        stackTraceSerialNumberOffset = classIDOffset + idSize;
        superClassIDOffset = stackTraceSerialNumberOffset + 4;
        classLoaderIDOffset = superClassIDOffset + idSize;
        signersID = classLoaderIDOffset + idSize;
        protectionDomainIDOffset = signersID + idSize;
        reserved1 = protectionDomainIDOffset + idSize;
        reserver2 = reserved1 + idSize;
        instanceSizeOffset = reserver2 + idSize;
        constantPoolSizeOffset = instanceSizeOffset + 4;

        fieldNameIDOffset = 0;
        fieldTypeOffset = fieldNameIDOffset + idSize;
        fieldValueOffset = fieldTypeOffset + 1;

        fieldSize = fieldTypeOffset + 1;

        minimumInstanceSize = 2 * idSize;

        fieldsCache = Collections.synchronizedMap(new FieldsCache());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    ClassDump getClassDumpByID(long classObjectID) {
        if (classObjectID == 0) {
            return null;
        }
        List allClasses = createClassCollection();
        LongMap.Entry entry = hprofHeap.idToOffsetMap.get(classObjectID);

        if (entry != null) {
            try {
                ClassDump dump = (ClassDump) allClasses.get(entry.getIndex() - 1);
                if (dump.fileOffset == entry.getOffset()) {
                    return dump;
                }
            } catch (IndexOutOfBoundsException ex) { // classObjectID do not reffer to ClassDump, its instance number is > classes.size()

                return null;
            } catch (ClassCastException ex) { // classObjectID do not reffer to ClassDump

                return null;
            }
        }

        return null;
    }

    JavaClass getJavaClassByName(String fqn) {
        Iterator classIt = createClassCollection().iterator();

        while (classIt.hasNext()) {
            ClassDump cls = (ClassDump) classIt.next();

            if (fqn.equals(cls.getName())) {
                return cls;
            }
        }

        return null;
    }

    Collection getJavaClassesByRegExp(String regexp) {
        Iterator classIt = createClassCollection().iterator();
        Collection result = new ArrayList(256);
        Pattern pattern = Pattern.compile(regexp);

        while (classIt.hasNext()) {
            ClassDump cls = (ClassDump) classIt.next();

            if (pattern.matcher(cls.getName()).matches()) {
                result.add(cls);
            }
        }
        return result;
    }

    int getMinimumInstanceSize() {
        return minimumInstanceSize;
    }

    ClassDump getPrimitiveArrayClass(byte type) {
        ClassDump primitiveArray = (ClassDump) primitiveArrayMap.get(Integer.valueOf(type));

        if (primitiveArray == null) {
            throw new IllegalArgumentException("Invalid type " + type); // NOI18N
        }

        return primitiveArray;
    }

    Map getClassIdToClassMap() {
        Collection allClasses = createClassCollection();
        Map map = new HashMap(allClasses.size()*4/3);
        Iterator classIt = allClasses.iterator();

        while(classIt.hasNext()) {
            ClassDump cls = (ClassDump) classIt.next();

            map.put(new Long(cls.getJavaClassId()),cls);
        }
        return map;
    }

    void addInstanceSize(ClassDump cls, int tag, long instanceOffset) {
        if ((tag == HprofHeap.OBJECT_ARRAY_DUMP) || (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP)) {
            Long sizeLong = (Long) arrayMap.get(cls);
            long size = 0;
            HprofByteBuffer dumpBuffer = hprofHeap.dumpBuffer;
            int idSize = dumpBuffer.getIDSize();
            long elementsOffset = instanceOffset + 1 + idSize + 4;

            if (sizeLong != null) {
                size = sizeLong.longValue();
            }

            int elements = dumpBuffer.getInt(elementsOffset);
            int elSize;

            if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
                elSize = hprofHeap.getValueSize(dumpBuffer.get(elementsOffset + 4));
            } else {
                elSize = idSize;
            }

            size += (getMinimumInstanceSize() + ArrayDump.HPROF_ARRAY_OVERHEAD + (((long)elements) * elSize));
            arrayMap.put(cls, Long.valueOf(size));
        }
    }

    synchronized List /*<JavaClass>*/ createClassCollection() {
        if (classes != null) {
            return classes;
        }

        classes = new ArrayList /*<JavaClass>*/(1000);

        long[] offset = new long[] { startOffset };

        while (offset[0] < endOffset) {
            long start = offset[0];
            int tag = hprofHeap.readDumpTag(offset);

            if (tag == HprofHeap.CLASS_DUMP) {
                ClassDump classDump = new ClassDump(this, start);
                long classId = classDump.getJavaClassId();
                LongMap.Entry classEntry = hprofHeap.idToOffsetMap.put(classId, start);

                classes.add(classDump);
                classEntry.setIndex(classes.size());
            }
        }

        hprofHeap.getLoadClassSegment().setLoadClassOffsets();
        arrayMap = new HashMap(classes.size() / 15);
        extractSpecialClasses();

        return classes;
    }

    private void extractSpecialClasses() {
        primitiveArrayMap = new HashMap();

        Iterator classIt = classes.iterator();

        while (classIt.hasNext()) {
            ClassDump jcls = (ClassDump) classIt.next();
            String vmName = jcls.getLoadClass().getVMName();
            Integer typeObj = null;

            if (vmName.equals("[Z")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BOOLEAN);
            } else if (vmName.equals("[C")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.CHAR);
            } else if (vmName.equals("[F")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.FLOAT);
            } else if (vmName.equals("[D")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.DOUBLE);
            } else if (vmName.equals("[B")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BYTE);
            } else if (vmName.equals("[S")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.SHORT);
            } else if (vmName.equals("[I")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.INT);
            } else if (vmName.equals("[J")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.LONG);
            } else if (vmName.equals("java/lang/Class")) { // NOI18N
                java_lang_Class = jcls;
            } else if (vmName.equals("boolean[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BOOLEAN);
            } else if (vmName.equals("char[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.CHAR);
            } else if (vmName.equals("float[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.FLOAT);
            } else if (vmName.equals("double[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.DOUBLE);
            } else if (vmName.equals("byte[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.BYTE);
            } else if (vmName.equals("short[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.SHORT);
            } else if (vmName.equals("int[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.INT);
            } else if (vmName.equals("long[]")) { // NOI18N
                typeObj = Integer.valueOf(HprofHeap.LONG);
            } else if (vmName.equals("java.lang.Class")) { // NOI18N
                java_lang_Class = jcls;
            }

            if (typeObj != null) {
                primitiveArrayMap.put(typeObj, jcls);
            }
        }
    }

    private static class FieldsCache extends LinkedHashMap {
        private static final int SIZE = 500;

        FieldsCache() {
            super(SIZE,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size() > SIZE) {
                return true;
            }
            return false;
        }
    }
}
