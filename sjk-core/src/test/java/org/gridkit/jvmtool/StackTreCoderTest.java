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
package org.gridkit.jvmtool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Assert;

public class StackTreCoderTest {

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
}
