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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPInputStream;

import org.gridkit.jvmtool.heapdump.io.ByteBufferPageManager;
import org.gridkit.jvmtool.heapdump.io.CompressdHprofByteBuffer;
import org.gridkit.jvmtool.heapdump.io.PagedFileHprofByteBuffer;


/**
 * This is factory class for creating {@link Heap} from the file in Hprof dump format.
 * @author Tomas Hurka
 */
public class HeapFactory {
    
    public static final long DEFAULT_BUFFER = 128 << 20;
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * this factory method creates {@link Heap} from a memory dump file in Hprof format.
     * <br>
     * <b>This implementation is using temporary disk files for building auxiliary indexes</b>
     * <br>
     * Speed: slow
     * @param heapDump file which contains memory dump
     * @return implementation of {@link Heap} corresponding to the memory dump
     * passed in heapDump parameter
     * @throws java.io.FileNotFoundException if heapDump file does not exist
     * @throws java.io.IOException if I/O error occurred while accessing heapDump file
     */
    public static Heap createHeap(File heapDump) throws FileNotFoundException, IOException {
        return createHeap(heapDump, 0);
    }

    /**
     * Fast {@link Heap} implementation is optimized for batch processing of dump.
     * Unlike normal {@link Heap} it doesn't create/use any temporary files. 
     */
    public static Heap createFastHeap(File heapDump) throws FileNotFoundException, IOException {
        return createFastHeap(heapDump, DEFAULT_BUFFER);
    }

    /**
     * Fast {@link Heap} implementation is optimized for batch processing of dump.
     * Unlike normal {@link Heap} it doesn't create/use any temporary files.
     * 
     * @param bufferSize if file can be mapped to memory no buffer would be used, overwise limits memory used for buffering
     */
    public static Heap createFastHeap(File heapDump, long bufferSize) throws FileNotFoundException, IOException {
        return new FastHprofHeap(createBuffer(heapDump, bufferSize), 0);
    }
    
    /**
     * this factory method creates {@link Heap} from a memory dump file in Hprof format.
     * If the memory dump file contains more than one dump, parameter segment is used to
     * select particular dump.
     * <br>
     * <b>This implementation is using temporary disk files for building auxiliary indexes</b>
     * <br>
     * Speed: slow
     * @return implementation of {@link Heap} corresponding to the memory dump
     * passed in heapDump parameter
     * @param segment select corresponding dump from multi-dump file
     * @param heapDump file which contains memory dump
     * @throws java.io.FileNotFoundException if heapDump file does not exist
     * @throws java.io.IOException if I/O error occurred while accessing heapDump file
     */
    public static Heap createHeap(File heapDump, int segment)
                           throws FileNotFoundException, IOException {
        return new HprofHeap(createBuffer(heapDump, DEFAULT_BUFFER), segment);
    }

    public static boolean canBeMemMapped(File heapDump) {
        
        try {
            if (isGZIP(heapDump)) {
                return false;
            }            
        }
        catch(NoClassDefFoundError e) {
            // GZip parser is not available
        }        
        
        try {
            FileInputStream fis = new FileInputStream(heapDump);
            FileChannel channel = fis.getChannel();
            long length = channel.size();
            int bufCount = (int)((length + ((1 << 30) - 1)) >> 30);
            MappedByteBuffer[] buffers = new MappedByteBuffer[bufCount];
            try {
                for(int i = 0; i != bufCount; ++i) {
                    long rm = length - (((long)i) << 30);
                    buffers[i] = channel.map(FileChannel.MapMode.READ_ONLY, ((long)i) << 30, Math.min(rm, 1 << 30));
                }
                return true;
            }
            catch(Exception e) {
                // ignore
            }
            finally {
                try {
                    channel.close();
                }
                catch(Exception e) {
                    // ignore
                }                
                try {
                    fis.close();
                }
                catch(Exception e) {
                    // ignore
                }                
                for(MappedByteBuffer mb: buffers) {
                    try {
                        callCleaner(mb);
                    }
                    catch(Exception e) {
                        // ignore
                    }                
                }
            }
        } catch (FileNotFoundException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }
        return false;
    }
    
    private static void callCleaner(MappedByteBuffer dumpBuffer) {
        Object c = rcall(dumpBuffer, "cleaner");
        rcall(c, "clean");        
    }
    
    private static Object rcall(Object o, String method, Object... args) {
        try {
            Class<?> c = o.getClass();
            Method m = getMethod(c, method);
            m.setAccessible(true);
            return m.invoke(o, args);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }

    private static Method getMethod(Class<?> c, String method) {
        for(Method m: c.getDeclaredMethods()) {
            if (m.getName().equals(method)) {
                return m;
            }
        }
        if (c.getSuperclass() != null) {
            return getMethod(c.getSuperclass(), method);
        }
        return null;
    }

    private static HprofByteBuffer createBuffer(File heapDump, long bufferSize) throws IOException {
        try {
            if (isGZIP(heapDump)) {
                return createCompressedHprofBuffer(heapDump, bufferSize);
            }            
        }
        catch(NoClassDefFoundError e) {
            // GZip parser is not available
        }

        HprofByteBuffer bb = HeapFactory.createHprofByteBuffer(heapDump, bufferSize);
        return bb;
    }

    private static HprofByteBuffer createCompressedHprofBuffer(File heapDump, long bufferSize) throws IOException, FileNotFoundException {
        return new CompressdHprofByteBuffer(new RandomAccessFile(heapDump, "r"), new ByteBufferPageManager(512 << 10, bufferSize));
    }
    
    private static boolean isGZIP(File headDump) {
        try {
            FileInputStream in = new FileInputStream(headDump);        
            GZIPInputStream is;
            try {
                is = new GZIPInputStream(in);
                is.read();
                is.close();
                return true;
            } catch (IOException e) {
                in.close();
            }
        } catch (IOException e) {
            // ignore
        }
        return false;
    }

    static HprofByteBuffer createHprofByteBuffer(File dumpFile, long bufferSize)
                                          throws IOException {
        long fileLen = dumpFile.length();
    
        if (fileLen < HprofByteBuffer.MINIMAL_SIZE) {
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
                return new PagedFileHprofByteBuffer(new RandomAccessFile(dumpFile, "r"), new ByteBufferPageManager(1 << 20, 1 << 20, bufferSize));
            }
    
            throw ex;
        }
    }
}
