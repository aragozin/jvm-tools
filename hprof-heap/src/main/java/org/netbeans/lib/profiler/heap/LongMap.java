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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * key - ID (long/int) of heap object
 * value (8/4) + 4 + 1 + (8/4)
 *  - offset (long/int) to dump file
 *  - instance index (int) - unique number of this {@link Instance} among all instances of the same Java Class
 *  - references flags (byte) - bit 0 set - has zero or one reference,
 *                            - bit 1 set - has GC root
 *                            - bit 2 set - tree object
 *  - ID/offset (long/int) - ID if reference flag bit 0 is set, otherwise offset to reference list file
 *  - retained size (long/int)
 *
 * @author Tomas Hurka
 */
class LongMap extends AbstractLongMap {

    private NumberList referenceList;

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class Entry extends AbstractLongMap.Entry {

        private static final byte NUMBER_LIST = 1;
        private static final byte GC_ROOT = 2;
        private static final byte TREE_OBJ = 4;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long offset;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Entry(long off) {
            offset = off;
        }

        private Entry(long off,long value) {
            offset = off;
            putFoffset(offset + KEY_SIZE, value);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        void setIndex(int index) {
            dumpBuffer.putInt(offset + KEY_SIZE + FOFFSET_SIZE, index);
        }

        int getIndex() {
            return dumpBuffer.getInt(offset + KEY_SIZE + FOFFSET_SIZE);
        }

        void setTreeObj() {
            byte flags = (byte)(getFlags() | TREE_OBJ);
            setFlags(flags);
        }

        boolean isTreeObj() {
            return (getFlags() & TREE_OBJ) != 0;
        }

        boolean hasOnlyOneReference() {
            return (getFlags() & NUMBER_LIST) == 0;
        }

        void setNearestGCRootPointer(long instanceId) {
            byte flags = (byte)(getFlags() | GC_ROOT);
            setFlags(flags);
            if ((flags & NUMBER_LIST) != 0) {   // put GC root pointer on the first place in references list
                try {
                    referenceList.putFirst(getReferencesPointer(),instanceId);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        long getNearestGCRootPointer() {
            try {
                byte flag = getFlags();
                if ((flag & GC_ROOT) != 0) { // has GC root pointer
                    long ref = getReferencesPointer();
                    if ((flag & NUMBER_LIST) != 0) { // get GC root pointer from number list
                        return referenceList.getFirstNumber(ref);
                    }
                    return ref;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return 0L;
        }

        void addReference(long instanceId) {
            try {
                byte flags = getFlags();
                long ref = getReferencesPointer();
                if ((flags & NUMBER_LIST) == 0) { // reference list is not used
                    if (ref == 0L) {    // no reference was set
                        setReferencesPointer(instanceId);
                    } else if (ref != instanceId) {    // one reference was set, switch to reference list
                       setFlags((byte)(flags | NUMBER_LIST));
                       long list = referenceList.addFirstNumber(ref,instanceId);
                       setReferencesPointer(list);
                    }
                } else { // use reference list
                    long newRef = referenceList.addNumber(ref,instanceId);
                    if (newRef != ref) {
                        setReferencesPointer(newRef);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        List<Long> getReferences() {
            byte flags = getFlags();
            long ref = getReferencesPointer();
            if ((flags & NUMBER_LIST) == 0) {
                if (ref == 0L) {
                    return Collections.<Long>emptyList();
                } else {
                    return Collections.singletonList(new Long(ref));
                }
            } else {
                try {
                    return referenceList.getNumbers(ref);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return Collections.<Long>emptyList();
        }

        long getOffset() {
            return getFoffset(offset + KEY_SIZE);
        }

        void setRetainedSize(long size) {
            if (FOFFSET_SIZE == 4) {
                dumpBuffer.putInt(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1 + ID_SIZE, (int)size);
            } else {
                dumpBuffer.putLong(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1 + ID_SIZE, size);
            }
        }

        long getRetainedSize() {
            if (FOFFSET_SIZE == 4) {
                return dumpBuffer.getInt(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1 + ID_SIZE);
            }
            return dumpBuffer.getLong(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1 + ID_SIZE);
        }

        private void setReferencesPointer(long instanceId) {
            putID(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1, instanceId);
        }

        private long getReferencesPointer() {
            return getID(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1);
        }

        private void setFlags(byte flags) {
            dumpBuffer.putByte(offset + KEY_SIZE + FOFFSET_SIZE + 4, flags);
        }

        private byte getFlags() {
            return dumpBuffer.getByte(offset + KEY_SIZE + FOFFSET_SIZE + 4);
        }
    }

    private static class RetainedSizeEntry implements Comparable<RetainedSizeEntry> {
        private final long instanceId;
        private final long retainedSize;

        private RetainedSizeEntry(long id,long size) {
            instanceId = id;
            retainedSize = size;
        }

        public int compareTo(RetainedSizeEntry o) {
            RetainedSizeEntry other = (RetainedSizeEntry) o;
            // bigger object are at beginning
            int diff = longCompare(other.retainedSize, retainedSize);
            if (diff == 0) {
                // sizes are the same, compare ids
                return longCompare(instanceId, other.instanceId);
            }
            return diff;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RetainedSizeEntry other = (RetainedSizeEntry) obj;
            return this.instanceId == other.instanceId;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + (int) (this.instanceId ^ (this.instanceId >>> 32));
            return hash;
        }
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LongMap(int size,int idSize,int foffsetSize) throws FileNotFoundException, IOException {
        super(size,idSize,foffsetSize,foffsetSize + 4 + 1 + idSize + foffsetSize);
        referenceList = new NumberList(ID_SIZE);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    Entry createEntry(long index) {
        return new Entry(index);
    }

    Entry createEntry(long index,long value) {
        return new Entry(index,value);
    }

    Entry get(long key) {
        return (Entry)super.get(key);
    }

    Entry put(long key, long value) {
        return (Entry)super.put(key,value);
    }

    void flush() {
        referenceList.flush();
    }

    long[] getBiggestObjectsByRetainedSize(int number) {
        SortedSet<RetainedSizeEntry> bigObjects = new TreeSet<RetainedSizeEntry>();
        long[] bigIds = new long[number];
        long min = 0;
        for (long index=0;index<fileSize;index+=ENTRY_SIZE) {
            long id = getID(index);
            if (id != 0) {
                long retainedSize = createEntry(index).getRetainedSize();
                if (bigObjects.size()<number) {
                    bigObjects.add(new RetainedSizeEntry(id,retainedSize));
                    min = ((RetainedSizeEntry)bigObjects.last()).retainedSize;
                } else if (retainedSize>min) {
                    bigObjects.remove(bigObjects.last());
                    bigObjects.add(new RetainedSizeEntry(id,retainedSize));
                    min = ((RetainedSizeEntry)bigObjects.last()).retainedSize;
                }
            }
        }
        int i = 0;
        Iterator<RetainedSizeEntry> it = bigObjects.iterator();
        while(it.hasNext()) {
            bigIds[i++]=((RetainedSizeEntry)it.next()).instanceId;
        }
        return bigIds;
    }

    private static int longCompare(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

}
