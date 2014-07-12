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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 *
 * @author Tomas Hurka
 */
class HprofFileBuffer extends HprofByteBuffer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int MAX_bufferSizeBits = 17;
    private static final int MIN_bufferSizeBits = 7;
    private static final int MIN_bufferSize = 1 << MIN_bufferSizeBits;
    private static final int MIN_bufferSizeMask = MIN_bufferSize - 1;
    private static final int BUFFER_EXT = 8;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    RandomAccessFile fis;
    private byte[] dumpBuffer;
    private long bufferStartOffset;
    private int bufferSizeBits;
    private int bufferSize;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofFileBuffer(File dumpFile) throws IOException {
        fis = new RandomAccessFile(dumpFile, "r");
        length = fis.length();
        bufferStartOffset = Long.MAX_VALUE;
        readHeader();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    synchronized char getChar(long index) {
        int i = loadBufferIfNeeded(index);
        int ch1 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch2 = ((int) dumpBuffer[i]) & 0xFF;

        return (char) ((ch1 << 8) + (ch2 << 0));
    }

    synchronized double getDouble(long index) {
        int i = loadBufferIfNeeded(index);

        return Double.longBitsToDouble(getLong(i));
    }

    synchronized float getFloat(long index) {
        int i = loadBufferIfNeeded(index);

        return Float.intBitsToFloat(getInt(i));
    }

    synchronized int getInt(long index) {
        int i = loadBufferIfNeeded(index);
        int ch1 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch2 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch3 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch4 = ((int) dumpBuffer[i]) & 0xFF;

        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    synchronized long getLong(long index) {
        return ((long) (getInt(index)) << 32) + (getInt(index + 4) & 0xFFFFFFFFL);
    }

    synchronized short getShort(long index) {
        int i = loadBufferIfNeeded(index);
        int ch1 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch2 = ((int) dumpBuffer[i]) & 0xFF;

        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    // delegate to MappedByteBuffer
    synchronized byte get(long index) {
        int i = loadBufferIfNeeded(index);

        return dumpBuffer[i];
    }

    synchronized void get(long position, byte[] chars) {
        int i = loadBufferIfNeeded(position);

        if ((i + chars.length) < dumpBuffer.length) {
            System.arraycopy(dumpBuffer, i, chars, 0, chars.length);
        } else {
            try {
                fis.seek(position);
                fis.readFully(chars);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void setBufferSize(long newBufferStart) {
        if ((newBufferStart > bufferStartOffset) && (newBufferStart < (bufferStartOffset + (2 * bufferSize)))) { // sequential read -> increase buffer size

            if (bufferSizeBits < MAX_bufferSizeBits) {
                setBufferSize(bufferSizeBits + 1);
            }
        } else { // reset buffer size
            setBufferSize(MIN_bufferSizeBits);
        }
    }

    private void setBufferSize(int newBufferSizeBits) {
        bufferSizeBits = newBufferSizeBits;
        bufferSize = 1 << bufferSizeBits;
        dumpBuffer = new byte[bufferSize + BUFFER_EXT];
    }

    private int loadBufferIfNeeded(long index) {
        if ((index >= bufferStartOffset) && (index < (bufferStartOffset + bufferSize))) {
            return (int) (index - bufferStartOffset);
        }

        long newBufferStart = index & ~MIN_bufferSizeMask;
        setBufferSize(newBufferStart);

        try {
            fis.seek(newBufferStart);
            fis.readFully(dumpBuffer);

            //System.out.println("Reading at "+newBufferStart+" size "+dumpBuffer.length+" thread "+Thread.currentThread().getName());
        } catch (EOFException ex) {
            // ignore
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        bufferStartOffset = newBufferStart;

        return (int) (index - bufferStartOffset);
    }
}
