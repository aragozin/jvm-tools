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

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 *
 * @author Tomas Hurka
 */
class StringSegment extends TagBounds {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final int UTF8CharsOffset;
    private final int lengthOffset;
    private final int stringIDOffset;
    private final int timeOffset;
    private LongHashMap stringIDMap;
    private HprofHeap hprofHeap;
    private Map<Long, String> stringCache = Collections.synchronizedMap(new StringCache());
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StringSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.STRING, start, end);

        int idSize = heap.dumpBuffer.getIDSize();
        hprofHeap = heap;
        timeOffset = 1;
        lengthOffset = timeOffset + 4;
        stringIDOffset = lengthOffset + 4;
        UTF8CharsOffset = stringIDOffset + idSize;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    String getStringByID(long stringID) {
        Long stringIDObj = new Long(stringID);
        String string = (String) stringCache.get(stringIDObj);
        if (string == null) {
            string = createStringByID(stringID);
            stringCache.put(stringIDObj,string);
        }
        return string;
    }

    private String createStringByID(long stringID) {
        return getString(getStringOffsetByID(stringID));
    }

    private String getString(long start) {
        HprofByteBuffer dumpBuffer = getDumpBuffer();

        if (start == -1) {
            return "<unknown string>"; // NOI18N
        }

        int len = dumpBuffer.getInt(start + lengthOffset);
        byte[] chars = new byte[len - dumpBuffer.getIDSize()];
        dumpBuffer.get(start + UTF8CharsOffset, chars);

        String s = "Error"; // NOI18N

        try {
            s = new String(chars, "UTF-8"); // NOI18N
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        return s;
    }

    private synchronized long getStringOffsetByID(long stringID) {
        long startLong;

        if (stringIDMap == null) {
            stringIDMap = new LongHashMap(32768);

            long[] offset = new long[] { startOffset };

            while (offset[0] < endOffset) {
                long start = offset[0];
                long sID = readStringTag(offset);
                stringIDMap.put(sID, start);
            }
        }

        startLong = stringIDMap.get(stringID);

        if (startLong == 0) {
            return -1;
        }

        return startLong;
    }

    private HprofByteBuffer getDumpBuffer() {
        HprofByteBuffer dumpBuffer = hprofHeap.dumpBuffer;

        return dumpBuffer;
    }

    private long readStringTag(long[] offset) {
        long start = offset[0];

        if (hprofHeap.readTag(offset) != HprofHeap.STRING) {
            return 0;
        }

        return getDumpBuffer().getID(start + stringIDOffset);
    }

    @SuppressWarnings("serial")
    private static class StringCache extends LinkedHashMap<Long, String> {
        private static final int SIZE = 1000;

        StringCache() {
            super(SIZE,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            if (size() > SIZE) {
                return true;
            }
            return false;
        }
    }

}
