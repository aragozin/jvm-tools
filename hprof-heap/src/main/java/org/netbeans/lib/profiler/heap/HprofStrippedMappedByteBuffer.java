/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Alexey Ragozin
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


/**
 * This version of byte buffer is using multiple
 * mapped byte buffers to workaround 2GiB limitation of {@link ByteBuffer}.
 *
 * <br/>
 *
 * Suitable only for 64bit architecture
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class HprofStrippedMappedByteBuffer extends HprofByteBuffer {

    private static int STRIPE_SHIFT = 30;
    private static int STRIPE_SIZE = 1 << STRIPE_SHIFT;
    private static long STRIPE_MASK = STRIPE_SIZE - 1;

    private MappedByteBuffer[] bufferStripe;

    HprofStrippedMappedByteBuffer(File dumpFile) throws IOException {
        FileInputStream fis = new FileInputStream(dumpFile);
        FileChannel channel = fis.getChannel();
        length = channel.size();

        int stripes = (int)((length + STRIPE_SIZE - 1) >> STRIPE_SHIFT);
        bufferStripe = new MappedByteBuffer[stripes];

        for(int i = 0; i != bufferStripe.length; ++i) {
            long offs = ((long)i) << STRIPE_SHIFT;
            long len = offs + STRIPE_SIZE <= length ? STRIPE_SIZE : length & STRIPE_MASK;
            bufferStripe[i] = channel.map(FileChannel.MapMode.READ_ONLY, offs, len);
        }

        channel.close();
        readHeader();
    }

    char getChar(long index) {
        try {
            return bufferStripe[stripeId(index)].getChar(stripeOffs(index));
        }
        catch(BufferUnderflowException e) {
            return slowGet(index, 2).getChar();
        }
    }

    double getDouble(long index) {
        try {
            return bufferStripe[stripeId(index)].getDouble(stripeOffs(index));
        }
        catch(BufferUnderflowException e) {
            return slowGet(index, 8).getDouble();
        }
    }

    float getFloat(long index) {
        try {
            return bufferStripe[stripeId(index)].getFloat(stripeOffs(index));
        }
        catch(BufferUnderflowException e) {
            return slowGet(index, 4).getFloat();
        }
    }

    int getInt(long index) {
        try {
            return bufferStripe[stripeId(index)].getInt(stripeOffs(index));
        }
        catch(BufferUnderflowException e) {
            return slowGet(index, 4).getInt();
        }
    }

    long getLong(long index) {
        try {
            return bufferStripe[stripeId(index)].getLong(stripeOffs(index));
        }
        catch(BufferUnderflowException e) {
            return slowGet(index, 8).getLong();
        }
    }

    short getShort(long index) {
        try {
            return bufferStripe[stripeId(index)].getShort(stripeOffs(index));
        }
        catch(BufferUnderflowException e) {
            return slowGet(index, 2).getShort();
        }
    }

    byte get(long index) {
        return bufferStripe[stripeId(index)].get(stripeOffs(index));
    }

    private int stripeId(long pos) {
        return (int)(pos >> STRIPE_SHIFT);
    }

    private int stripeOffs(long pos) {
        return (int)(pos & STRIPE_MASK);
    }

    private ByteBuffer slowGet(long position, int len) {
        byte[] buf = new byte[len];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        get(position, buf);
        return bb;
    }

    void get(long position, byte[] chars) {
        try {
            MappedByteBuffer buffer = bufferStripe[stripeId(position)];
            buffer.position(stripeOffs(position));
            buffer.get(chars);
        }
        catch(BufferUnderflowException e) {
            // catch cross stripe access
            int s = stripeId(position);
            if (s == bufferStripe.length - 1) {
                // last stripe
                throw e;
            }
            int pos = stripeOffs(position);
            int brk = STRIPE_SIZE - (int)(position & STRIPE_MASK);
            bufferStripe[s].position(pos);
            bufferStripe[s].get(chars, 0, brk);
            bufferStripe[s + 1].position(0);
            bufferStripe[s + 1].get(chars, brk, chars.length - brk);
        }
    }
}
