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


/**
 * key - ID (long/int) of heap object
 * value (8/4) + 4 + 1 + (8/4)
 *  - offset (long/int) to dump file
 *  - instance index (int) - unique number of this {@link Instance} among all instances of the same Java Class
 *  - references flags (byte) - bit 0 set - has zero or one reference,
 *                            - bit 1 set - has GC root
 *                            - bit 2 set - tree object
 *  - ID/offset (long/int) - ID if reference flag bit 0 is set, otherwise offset to reference list file
 *  - retained size
 *
 * @author Tomas Hurka
 */
class DomMap extends AbstractLongMap {


    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class Entry extends AbstractLongMap.Entry {

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long offset;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Entry(long off) {
            offset = off;
        }

        private Entry(long off,long value) {
            offset = off;
            setIdom(value);
        }
        //~ Methods --------------------------------------------------------------------------------------------------------------

        void setIdom(long instanceId) {
            putID(offset + KEY_SIZE, instanceId);
        }

        long getIdom() {
            return getID(offset + KEY_SIZE);
        }
    }


    //~ Constructors -------------------------------------------------------------------------------------------------------------

    DomMap(int size,int idSize,int foffsetSize) throws FileNotFoundException, IOException {
        super(size,idSize,foffsetSize,idSize);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    Entry createEntry(long index) {
        return new Entry(index);
    }

    Entry createEntry(long index, long value) {
        return new Entry(index,value);
    }

    Entry get(long key) {
        return (Entry)super.get(key);
    }

    Entry put(long key, long value) {
        return (Entry)super.put(key,value);
    }


}
