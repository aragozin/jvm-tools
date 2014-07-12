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

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Tomas Hurka
 */
class ThreadObjectHprofGCRoot extends HprofGCRoot implements ThreadObjectGCRoot {

    ThreadObjectHprofGCRoot(HprofHeap h, long offset) {
        super(h,offset);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public StackTraceElement[] getStackTrace() {
        int stackTraceSerialNumber = getStackTraceSerialNumber();

        if (stackTraceSerialNumber != 0) {
            StackTrace stackTrace = heap.getStackTraceSegment().getStackTraceBySerialNumber(stackTraceSerialNumber);
            if (stackTrace != null) {
                StackFrame[] frames = stackTrace.getStackFrames();
                StackTraceElement[] stackElements = new StackTraceElement[frames.length];

                for (int i=0;i<frames.length;i++) {
                    StackFrame f = frames[i];
                    String className = f.getClassName();
                    String method = f.getMethodName();
                    String source = f.getSourceFile();
                    int number = f.getLineNumber();

                    if (number == StackFrame.NATIVE_METHOD) {
                        number = -2;
                    } else if (number == StackFrame.NO_LINE_INFO || number == StackFrame.UNKNOWN_LOCATION) {
                        number = -1;
                    }
                    stackElements[i] = new StackTraceElement(className,method,source,number);
                }
                return stackElements;
            }
        }
        return null;
    }

    int getThreadSerialNumber() {
        return heap.dumpBuffer.getInt(fileOffset + 1 + heap.dumpBuffer.getIDSize());
    }

    private int getStackTraceSerialNumber() {
        return heap.dumpBuffer.getInt(fileOffset + 1 + heap.dumpBuffer.getIDSize() + 4);
    }

}
