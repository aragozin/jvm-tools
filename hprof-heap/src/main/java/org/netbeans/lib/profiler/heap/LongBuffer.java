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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * LongBuffer is a special kind of buffer for storing longs. It uses array of longs if there is only few longs
 * stored, otherwise longs are saved to backing temporary file.
 * @author Tomas Hurka
 */
class LongBuffer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DataInputStream readStream;
    private boolean readStreamClosed;
    private DataOutputStream writeStream;
    private File backingFile;
    private long[] buffer;
    private boolean useBackingFile;
    private int bufferSize;
    private int readOffset;
    private int longs;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LongBuffer(int size) {
        buffer = new long[size];
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    void delete() {
        if (backingFile != null) {
            backingFile.delete();
        }
    }

    boolean hasData() {
        return longs > 0;
    }

    long readLong() throws IOException {
        if (!useBackingFile) {
            if (readOffset < bufferSize) {
                return buffer[readOffset++];
            } else {
                return 0;
            }
        }
        if (readStreamClosed) {
            return 0;
        }
        try {
            return readStream.readLong();
        } catch (EOFException ex) {
            readStreamClosed = true;
            readStream.close();
            return 0L;
        }
    }

    void reset() throws IOException {
        bufferSize = 0;
        if (writeStream != null) {
            writeStream.close();
        }
        if (readStream != null) {
            readStream.close();
        }
        writeStream = null;
        readStream = null;
        readStreamClosed = false;
        longs = 0;
        useBackingFile = false;
        readOffset = 0;
    }

    void startReading() {
        if (useBackingFile) {
            try {
                writeStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        writeStream = null;
        rewind();
    }

    void rewind() {
        readOffset = 0;

        if (useBackingFile) {
            try {
                if (readStream != null) {
                    readStream.close();
                }
                readStream = new DataInputStream(new BufferedInputStream(new FileInputStream(backingFile), buffer.length * 8));
                readStreamClosed = false;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    void writeLong(long data) throws IOException {
        longs++;
        if (bufferSize < buffer.length) {
            buffer[bufferSize++] = data;
            return;
        }

        if (backingFile == null) {
            backingFile = File.createTempFile("NBProfiler", ".gc"); // NOI18N
            backingFile.deleteOnExit();
        }

        if (writeStream == null) {
            writeStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(backingFile), buffer.length * 8));

            for (int i = 0; i < buffer.length; i++) {
                writeStream.writeLong(buffer[i]);
            }

            useBackingFile = true;
        }

        writeStream.writeLong(data);
    }

    LongBuffer revertBuffer() throws IOException {
        LongBuffer reverted = new LongBuffer(buffer.length);

        if (bufferSize < buffer.length) {
            for (int i=0;i<bufferSize;i++) {
                reverted.writeLong(buffer[bufferSize - 1 - i]);
            }
        } else {
            writeStream.flush();
            RandomAccessFile raf = new RandomAccessFile(backingFile,"r");
            long offset = raf.length();
            while(offset > 0) {
                offset-=8;
                raf.seek(offset);
                reverted.writeLong(raf.readLong());
            }
        }
        reverted.startReading();
        return reverted;
    }

    int getSize() {
        return longs;
    }

}
