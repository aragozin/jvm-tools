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


/**
 *
 * @author Tomas Hurka
 */
class LoadClass extends HprofObject {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final LoadClassSegment loadClassSegment;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LoadClass(LoadClassSegment segment, long offset) {
        super(offset);
        loadClassSegment = segment;
        assert getHprofBuffer().get(offset) == HprofHeap.LOAD_CLASS;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    long getClassObjectID() {
        return getHprofBuffer().getID(fileOffset + loadClassSegment.classIDOffset);
    }

    String getName() {
        return convertToName(getVMName());
    }

    long getNameID() {
        return getHprofBuffer().getID(fileOffset + loadClassSegment.nameStringIDOffset);
    }

    String getVMName() {
        StringSegment stringSegment = loadClassSegment.hprofHeap.getStringSegment();

        return stringSegment.getStringByID(getNameID());
    }

    private HprofByteBuffer getHprofBuffer() {
        return loadClassSegment.hprofHeap.dumpBuffer;
    }

    private static String convertToName(String vmName) {
        String name = vmName.replace('/', '.'); // NOI18N
        int i;

        for (i = 0; i < name.length(); i++) {
            if (name.charAt(i) != '[') { // NOI18N    // arrays
                break;
            }
        }

        if (i != 0) {
            name = name.substring(i);

            char firstChar = name.charAt(0);

            if (firstChar == 'L') { // NOI18N      // object array
                name = name.substring(1, name.length() - 1);
            } else {
                switch (firstChar) {
                    case 'C':
                        name = "char"; // NOI18N
                        break;
                    case 'B':
                        name = "byte"; // NOI18N
                        break;
                    case 'I':
                        name = "int"; // NOI18N
                        break;
                    case 'Z':
                        name = "boolean"; // NOI18N
                        break;
                    case 'F':
                        name = "float"; // NOI18N
                        break;
                    case 'D':
                        name = "double"; // NOI18N
                        break;
                    case 'S':
                        name = "short"; // NOI18N
                        break;
                    case 'J':
                        name = "long"; // NOI18N
                        break;
                }
            }

            for (; i > 0; i--) {
                name = name.concat("[]"); // NOI18N
            }
        }

        return name;
    }
}
