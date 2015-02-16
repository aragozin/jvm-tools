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
package org.gridkit.jvmtool.stacktrace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

public class StackTreeCoderTest {

    @Test
    public void test() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2014, 01, 01);
        long time = cal.getTimeInMillis();

        System.out.println(time);
    }


//    @Test
    public void verifyVarIntEncoding() throws IOException {

        int n = 0;
        int j = 0;
        ByteArrayOutputStream bos;
        DataOutputStream dos;
        bos = new ByteArrayOutputStream(4 << 20);
        dos = new DataOutputStream(bos);
        for(int i = 0; i != (1 << 30) + 1000000; ++i) {
            StackTraceCodec.writeVarInt(dos, i);
            ++n;
            if (n == 1000000) {
                System.out.println(i);
                n = 0;
                byte[] buf = bos.toByteArray();
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
                for(;j <= i; ++j) {
                    int x = StackTraceCodec.readVarInt(dis);
                    Assert.assertEquals(j, x);
                }
                bos = new ByteArrayOutputStream(4 << 20);
                dos = new DataOutputStream(bos);
            }
        }
    }

    @Test
    public void verifyVarLongBoundaries() throws IOException {
        ByteArrayOutputStream bos;
        DataOutputStream dos;
        bos = new ByteArrayOutputStream(4 << 20);
        dos = new DataOutputStream(bos);

        StackTraceCodec.writeVarLong(dos, 0);
        StackTraceCodec.writeVarLong(dos, 1);
        StackTraceCodec.writeVarLong(dos, -1);
        StackTraceCodec.writeVarLong(dos, Long.MAX_VALUE);
        StackTraceCodec.writeVarLong(dos, Long.MIN_VALUE);
        StackTraceCodec.writeVarLong(dos, (0xFFFFFFFFl & Integer.MAX_VALUE));
        StackTraceCodec.writeVarLong(dos, (0xFFFFFFFFl & Integer.MIN_VALUE));

        byte[] buf = bos.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));

        Assert.assertEquals(0, StackTraceCodec.readVarLong(dis));
        Assert.assertEquals(1, StackTraceCodec.readVarLong(dis));
        Assert.assertEquals(-1, StackTraceCodec.readVarLong(dis));
        Assert.assertEquals(Long.MAX_VALUE, StackTraceCodec.readVarLong(dis));
        Assert.assertEquals(Long.MIN_VALUE, StackTraceCodec.readVarLong(dis));
        Assert.assertEquals((0xFFFFFFFFl & Integer.MAX_VALUE), StackTraceCodec.readVarLong(dis));
        Assert.assertEquals((0xFFFFFFFFl & Integer.MIN_VALUE), StackTraceCodec.readVarLong(dis));

    }

//    @Test
    public void verifyVarLongEncoding() throws IOException {

        Random rnd = new Random(1);

        int n = 0;
        int j = 0;
        ByteArrayOutputStream bos;
        DataOutputStream dos;
        bos = new ByteArrayOutputStream(4 << 20);
        dos = new DataOutputStream(bos);
        long[] lbuf = new long[1 << 20];

        for(int i = 0; i != 1 << 30 ; ++i) {
            lbuf[n] = rnd.nextLong();
            StackTraceCodec.writeVarLong(dos, lbuf[n]);
            ++n;
            if (n == lbuf.length) {
//                System.out.println(i);
                n = 0;
                byte[] buf = bos.toByteArray();
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
                for(j = 0;j != lbuf.length; ++j) {
                    long x = StackTraceCodec.readVarLong(dis);
                    Assert.assertEquals(lbuf[j], x);
                }
                bos = new ByteArrayOutputStream(4 << 20);
                dos = new DataOutputStream(bos);
            }
        }
    }
}
