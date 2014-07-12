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
class HprofInstanceValue extends HprofObject implements FieldValue {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofField field;
    InstanceDump instance;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofInstanceValue(InstanceDump i, HprofField f, long offset) {
        super(offset);
        instance = i;
        field = f;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Instance getDefiningInstance() {
        return instance;
    }

    public Field getField() {
        return field;
    }

    public String getValue() {
        return getTypeValue().toString();
    }

    Object getTypeValue() {
        byte type = field.getValueType();
        HprofByteBuffer dumpBuffer = instance.dumpClass.getHprofBuffer();

        return getTypeValue(dumpBuffer, fileOffset, type);
    }

    static Object getTypeValue(final HprofByteBuffer dumpBuffer, final long position, final byte type) {
        switch (type) {
            case HprofHeap.OBJECT:

                long obj = dumpBuffer.getID(position);

                return new Long(obj);
            case HprofHeap.BOOLEAN:

                byte b = dumpBuffer.get(position);

                return Boolean.valueOf(b != 0);
            case HprofHeap.CHAR:

                char ch = dumpBuffer.getChar(position);

                return Character.valueOf(ch);
            case HprofHeap.FLOAT:

                float f = dumpBuffer.getFloat(position);

                return new Float(f);
            case HprofHeap.DOUBLE:

                double d = dumpBuffer.getDouble(position);

                return new Double(d);
            case HprofHeap.BYTE:

                byte bt = dumpBuffer.get(position);

                return new Byte(bt);
            case HprofHeap.SHORT:

                short sh = dumpBuffer.getShort(position);

                return new Short(sh);
            case HprofHeap.INT:

                int i = dumpBuffer.getInt(position);

                return Integer.valueOf(i);
            case HprofHeap.LONG:

                long lg = dumpBuffer.getLong(position);

                return new Long(lg);
            default:
                return "Invalid type " + type; // NOI18N
        }
    }
}
