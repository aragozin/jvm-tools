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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Tomas Hurka
 */
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
class DominatorTree {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int BUFFER_SIZE = (64 * 1024) / 8;
    private static final int ADDITIONAL_IDS_THRESHOLD = 30;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HprofHeap heap;
    private LongBuffer multipleParents;
    private LongBuffer revertedMultipleParents;
    private LongBuffer currentMultipleParents;
    private Map<Long,Long> map;
    private Set dirtySet = Collections.EMPTY_SET;
    private Map canContainItself;
    private Map nearestGCRootCache = new NearestGCRootCache(400000);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    DominatorTree(HprofHeap h, LongBuffer multiParents) {
        heap = h;
        multipleParents = multiParents;
        currentMultipleParents = multipleParents;
        map = new HashMap(multiParents.getSize());
        try {
            revertedMultipleParents = multiParents.revertBuffer();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getLocalizedMessage(),ex);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    synchronized void computeDominators() {
        boolean changed = true;
        boolean igonoreDirty;
        try {
            do {
                currentMultipleParents.rewind();
                igonoreDirty = !changed;
                changed = computeOneLevel(igonoreDirty);
                switchParents();
            } while (changed || !igonoreDirty);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        deleteBuffers();
        dirtySet = Collections.EMPTY_SET;
    }


    private boolean computeOneLevel(boolean ignoreDirty) throws IOException {
        boolean changed = false;
        Set newDirtySet = new HashSet(map.size()/10);
        List<Long> additionalIds = new ArrayList();
        int additionalIndex = 0;
        // debug
//        long processedId = 0;
//        long changedId = 0;
//        long index = 0;
//        List<Long> changedIds = new ArrayList();
//        List<Long> changedIdx = new ArrayList();
//        List<Boolean> addedBynewDirtySet = new ArrayList();
//        List<Long> oldDomIds = new ArrayList();
//        List<Long> newDomIds = new ArrayList();

//System.out.println("New level, dirtyset size: "+dirtySet.size());
        for (;;) {
            long instanceId = readLong();
            if (instanceId == 0) {  // end of level
                if (additionalIndex >= additionalIds.size()) {
                    if (additionalIndex>0) {
//System.out.println("Additional instances "+additionalIndex);
                    }
                    break;
                }
                instanceId = additionalIds.get(additionalIndex++).longValue();
            }
            Long instanceIdObj = new Long(instanceId);
            Long oldIdomObj = (Long) map.get(instanceIdObj);
//index++;
            if (oldIdomObj == null || (oldIdomObj.longValue() != 0 && (ignoreDirty || dirtySet.contains(oldIdomObj) || dirtySet.contains(instanceIdObj)))) {
//processedId++;
                LongMap.Entry entry = heap.idToOffsetMap.get(instanceId);
                List refs = entry.getReferences();
                Iterator refIt = refs.iterator();
                Long newIdomIdObj = (Long)refIt.next();
                boolean dirty = false;

                while(refIt.hasNext() && newIdomIdObj.longValue() != 0) {
                    Long refIdObj = (Long)refIt.next();
                    newIdomIdObj = intersect(newIdomIdObj,refIdObj);
                }
                if (oldIdomObj == null) {
//addedBynewDirtySet.add((newDirtySet.contains(oldIdomObj) || newDirtySet.contains(instanceIdObj)) && !(dirtySet.contains(oldIdomObj) || dirtySet.contains(instanceIdObj)));
                    map.put(instanceIdObj, newIdomIdObj);
                    newDirtySet.add(newIdomIdObj);
                    changed = true;
//changedId++;
//changedIds.add(instanceIdObj);
//changedIdx.add(index);
//oldDomIds.add(null);
//newDomIds.add(newIdomIdObj);
                } else if (oldIdomObj.longValue() != newIdomIdObj.longValue()) {
//addedBynewDirtySet.add((newDirtySet.contains(oldIdomObj) || newDirtySet.contains(instanceIdObj)) && !(dirtySet.contains(oldIdomObj) || dirtySet.contains(instanceIdObj)));
                    newDirtySet.add(oldIdomObj);
                    newDirtySet.add(newIdomIdObj);
                    map.put(instanceIdObj,newIdomIdObj);
                    if (dirtySet.size() < ADDITIONAL_IDS_THRESHOLD) {
                        updateAdditionalIds(instanceId, additionalIds);
                    }
                    changed = true;
//changedId++;
//changedIds.add(instanceIdObj);
//changedIdx.add(index);
//oldDomIds.add(oldIdomObj);
//newDomIds.add(newIdomIdObj);
                }
            }
        }
        dirtySet = newDirtySet;
//System.out.println("Processed: "+processedId);
//System.out.println("Changed:   "+changedId);
//System.out.println("-------------------");
//printObjs(changedIds,oldDomIds,newDomIds, addedBynewDirtySet, changedIdx);
//System.out.println("-------------------");
        return changed;
    }

    private void updateAdditionalIds(final long instanceId, final List<Long> additionalIds) {
        Instance i = heap.getInstanceByID(instanceId);
//System.out.println("Inspecting "+printInstance(instanceIdObj));
        if (i != null) {
            for (Object v : i.getFieldValues()) {
                if (v instanceof ObjectFieldValue) {
                    Instance val = ((ObjectFieldValue)v).getInstance();
                    if (val != null) {
                        long idp = val.getInstanceId();
                        Long idO = new Long(idp);
                        Long idomO = map.get(idO);
                        if (idomO != null && idomO.longValue() != 0) {
                            additionalIds.add(idO);
//System.out.println("  Adding "+printInstance(idO));
                        }
                    }
                }
            }
        }
    }

    private void deleteBuffers() {
        multipleParents.delete();
        revertedMultipleParents.delete();
    }

    private long readLong() throws IOException {
        return currentMultipleParents.readLong();
    }

    long getIdomId(long instanceId, LongMap.Entry entry) {
        Long idomEntry = (Long) map.get(new Long(instanceId));
        if (idomEntry != null) {
            return idomEntry.longValue();
        }
        if (entry == null) {
            entry = heap.idToOffsetMap.get(instanceId);
        }
        return entry.getNearestGCRootPointer();
    }

    boolean hasInstanceInChain(int tag, Instance i) {
        ClassDump javaClass;
        long idom;
        long instanceId;

        if (tag == HprofHeap.PRIMITIVE_ARRAY_DUMP) {
            return false;
        }
        javaClass = (ClassDump) i.getJavaClass();
        if (canContainItself == null) {
            canContainItself = new HashMap(heap.getAllClasses().size()/2);
        }
        if (tag == HprofHeap.INSTANCE_DUMP) {
            Boolean canContain = (Boolean) canContainItself.get(javaClass);

            if (canContain == null) {
                canContain = Boolean.valueOf(javaClass.canContainItself());
                canContainItself.put(javaClass,canContain);
            }
            if (!canContain.booleanValue()) {
                return false;
            }
        }
        instanceId = i.getInstanceId();
        idom = getIdomId(instanceId);
        for (;idom!=0;idom=getIdomId(idom)) {
            Instance ip = heap.getInstanceByID(idom);
            JavaClass cls = ip.getJavaClass();

            if (javaClass.equals(cls)) {
                return true;
            }
        }
        return false;
    }

    private Long getNearestGCRootPointer(Long instanceIdLong) {
        LongMap.Entry entry;
        Long nearestGCLong = (Long) nearestGCRootCache.get(instanceIdLong);
        Long nearestGC;
        if (nearestGCLong != null) {
            return nearestGCLong;
        }
        entry = heap.idToOffsetMap.get(instanceIdLong.longValue());
        nearestGC = Long.valueOf(entry.getNearestGCRootPointer());
        nearestGCRootCache.put(instanceIdLong,nearestGC);
        return nearestGC;
    }

    private Long getIdomId(Long instanceIdLong) {
        Long idomObj = (Long) map.get(instanceIdLong);

        if (idomObj != null) {
            return idomObj;
        }
        return getNearestGCRootPointer(instanceIdLong);
    }

    private Long intersect(Long idomIdObj, Long refIdObj) {
        if (idomIdObj.longValue() == refIdObj.longValue()) {
            return idomIdObj;
        }
        if (idomIdObj.longValue() == 0 || refIdObj.longValue() == 0) {
            return Long.valueOf(0);
        }
        Set leftIdoms = new HashSet(200);
        Set rightIdoms = new HashSet(200);
        Long leftIdomObj = idomIdObj;
        Long rightIdomObj = refIdObj;


        leftIdoms.add(leftIdomObj);
        rightIdoms.add(rightIdomObj);
        while(true) {
            if (leftIdomObj.longValue() != 0) {
                leftIdomObj = getIdomId(leftIdomObj);
                if (rightIdoms.contains(leftIdomObj)) {
                    return leftIdomObj;
                }
                leftIdoms.add(leftIdomObj);
            }
            if (rightIdomObj.longValue() != 0) {
                rightIdomObj = getIdomId(rightIdomObj);
                if (leftIdoms.contains(rightIdomObj)) {
                    return rightIdomObj;
                }
                rightIdoms.add(rightIdomObj);
            }
        }
    }

    private void switchParents() {
        if (currentMultipleParents == revertedMultipleParents) {
            currentMultipleParents = multipleParents;
        } else {
            currentMultipleParents = revertedMultipleParents;
        }
    }

    // debugging
    private void printObjs(List<Long> changedIds, List<Long> oldDomIds, List<Long> newDomIds, List<Boolean> addedByDirtySet, List<Long> changedIdx) {
        if (changedIds.size()>20) return;
        TreeMap<Integer,String> m = new TreeMap();

        for (int i=0; i<changedIds.size(); i++) {
            Long iid = changedIds.get(i);
            Long oldDom = oldDomIds.get(i);
            Long newDom = newDomIds.get(i);
            Long index = changedIdx.get(i);
            Boolean addedByDirt = addedByDirtySet.get(i);
            Instance ii = heap.getInstanceByID(iid.longValue());
            int number = ii.getInstanceNumber();
            String text = "Index: "+index+(addedByDirt?" New ":" Old ")+printInstance(iid);

            text+=" OldDom "+printInstance(oldDom);
            text+=" NewDom: "+printInstance(newDom);
            m.put(number,text);
        }
        for (Integer in : m.keySet()) {
            System.out.println(m.get(in));
        }
    }

    // debugging
    String printInstance(Long instanceid) {
        if (instanceid == null || instanceid.longValue() == 0) {
            return "null";
        }
        Instance ii = heap.getInstanceByID(instanceid.longValue());
        return ii.getJavaClass().getName()+"#"+ii.getInstanceNumber();

    }

    @SuppressWarnings("serial")
    private static final class NearestGCRootCache extends LinkedHashMap {
        private final int maxSize;

        private NearestGCRootCache(int size) {
            super(size,0.75F,true);
            maxSize = size;
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxSize;
        }

    }
}
