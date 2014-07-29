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
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.lab.jvm.attach.HeapDumper;

public class HeapDumpProcuder {

    private static int PID;
    static {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        PID = Integer.valueOf(pid.substring(0, pid.indexOf('@')));
    }

    private static String HEAP_DUMP_PATH = "target/dump/test.dump";

    public static File getHeapDump() {
        File file = new File(HEAP_DUMP_PATH);
        if (!file.exists()) {
            System.out.println("Generating heap dump: " + HEAP_DUMP_PATH);
            initTestHeap();
            System.out.println(HeapDumper.dumpLive(PID, HEAP_DUMP_PATH, 120000));
        }
        return file;
    }

    static List<DummyA> dummyA = new ArrayList<DummyA>();
    static List<DummyB> dummyB = new ArrayList<DummyB>();
    static DummyC dummyC = new DummyC();

    public static void initTestHeap() {

        for(int i = 0; i != 50; ++i) {
            dummyA.add(new DummyA());
        }

        for(int i = 0; i != 50; ++i) {
            DummyB dmb = new DummyB();
            dmb.seqNo = String.valueOf(i);
            for(int j = 0; j != i; ++j) {
                dmb.list.add(String.valueOf(j));
                dmb.map.put("k" + String.valueOf(j), "v" + String.valueOf(j));
            }
            dummyB.add(dmb);
        }
    }
}
