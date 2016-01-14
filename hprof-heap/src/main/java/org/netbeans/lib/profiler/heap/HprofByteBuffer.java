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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.ResourceBundle;

import org.gridkit.jvmtool.heapdump.PagedFileHprofByteBuffer;


/**
 *
 * @author Tomas Hurka
 */
public abstract class HprofByteBuffer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Magic header
    static final String magic1 = "JAVA PROFILE 1.0.1"; // NOI18N
    static final String magic2 = "JAVA PROFILE 1.0.2"; // NOI18N
    static final int JAVA_PROFILE_1_0_1 = 1;
    static final int JAVA_PROFILE_1_0_2 = 2;
    static final int MINIMAL_SIZE = 30;
    static final boolean DEBUG = false;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    int idSize;
    int version;
    long headerSize;
    long length;
    long time;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    static HprofByteBuffer createHprofByteBuffer(File dumpFile)
                                          throws IOException {
        long fileLen = dumpFile.length();

        if (fileLen < MINIMAL_SIZE) {
            String errText = "File size is too small";
            throw new IOException(errText);
        }

        try {
            if (fileLen < Integer.MAX_VALUE) {
                return new HprofMappedByteBuffer(dumpFile);
            } else {
                return new HprofLongMappedByteBuffer(dumpFile);
            }
        } catch (IOException ex) {
            if (ex.getCause() instanceof OutOfMemoryError) { // can happen on 32bit Windows, since there is only 2G for memory mapped data for whole java process.
                return new PagedFileHprofByteBuffer(new RandomAccessFile(dumpFile, "r"), 4 << 20, 16);
            }

            throw ex;
        }
    }

    abstract char getChar(long index);

    abstract double getDouble(long index);

    abstract float getFloat(long index);

    long getHeaderSize() {
        return headerSize;
    }

    long getID(long offset) {
        if (idSize == 4) {
            return ((long)getInt(offset)) & 0xFFFFFFFFL;
        } else if (idSize == 8) {
            return getLong(offset);
        }
        assert false;

        return -1;
    }

    int getIDSize() {
        return idSize;
    }

    int getFoffsetSize() {
        return length<Integer.MAX_VALUE ? 4 : 8;
    }

    abstract int getInt(long index);

    abstract long getLong(long index);

    abstract short getShort(long index);

    long getTime() {
        return time;
    }

    long capacity() {
        return length;
    }

    abstract byte get(long index);

    abstract void get(long position, byte[] chars);

    void readHeader() throws IOException {
        long[] offset = new long[1];
        String magic = readStringNull(offset, MINIMAL_SIZE);

        if (DEBUG) {
            System.out.println("Magic " + magic); // NOI18N
        }

        if (magic1.equals(magic)) {
            version = JAVA_PROFILE_1_0_1;
        } else if (magic2.equals(magic)) {
            version = JAVA_PROFILE_1_0_2;
        } else {
            if (DEBUG) {
                System.out.println("Invalid version"); // NOI18N
            }

            String errText = ResourceBundle.getBundle("org/netbeans/lib/profiler/heap/Bundle")
                                           .getString("HprofByteBuffer_InvalidFormat");
            throw new IOException(errText);
        }

        idSize = getInt(offset[0]);
        offset[0] += 4;
        time = getLong(offset[0]);
        offset[0] += 8;

        if (DEBUG) {
            System.out.println("ID " + idSize); // NOI18N
        }

        if (DEBUG) {
            System.out.println("Date " + new Date(time).toString()); // NOI18N
        }

        headerSize = offset[0];
    }

    private String readStringNull(long[] offset, int len) {
        StringBuilder s = new StringBuilder(20);
        byte b = get(offset[0]++);

        for (; (b > 0) && (s.length() < len); b = get(offset[0]++)) {
            s.append((char) b);
        }

        return s.toString();
    }
}
