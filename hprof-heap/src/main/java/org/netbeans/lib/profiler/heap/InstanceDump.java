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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Tomas Hurka
 */
class InstanceDump extends HprofObject implements Instance {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    final ClassDump dumpClass;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    InstanceDump(ClassDump cls, long offset) {
        super(offset);
        dumpClass = cls;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public List<FieldValue> getFieldValues() {
        long offset = fileOffset + getInstanceFieldValuesOffset();
        List<Field> fields = dumpClass.getAllInstanceFields();
        List<FieldValue> values = new ArrayList<FieldValue>(fields.size());
        Iterator<Field> fit = fields.iterator();

        while (fit.hasNext()) {
            HprofField field = (HprofField) fit.next();

            if (field.getValueType() == HprofHeap.OBJECT) {
                values.add(new HprofInstanceObjectValue(this, field, offset));
            } else {
                values.add(new HprofInstanceValue(this, field, offset));
            }

            offset += field.getValueSize();
        }

        return values;
    }

    public boolean isGCRoot() {
        return getHprof().getGCRoot(this) != null;
    }

    public long getInstanceId() {
        return dumpClass.getHprofBuffer().getID(fileOffset + 1);
    }

    public int getInstanceNumber() {
        return getHprof().idToInstanceNumber(getInstanceId());
    }

    public JavaClass getJavaClass() {
        return dumpClass;
    }

    public Instance getNearestGCRootPointer() {
        return getHprof().getNearestGCRootPointer(this);
    }

    public long getReachableSize() {
        return 0;
    }

    public List<Value> getReferences() {
        return getHprof().findReferencesFor(getInstanceId());
    }

    public long getRetainedSize() {
        return getHprof().getRetainedSize(this);
    }

    public long getSize() {
        return dumpClass.getInstanceSize();
    }

    public List<FieldValue> getStaticFieldValues() {
        return dumpClass.getStaticFieldValues();
    }

    public Object getValueOfField(String name) {
        Iterator<FieldValue> fIt = getFieldValues().iterator();
        FieldValue matchingFieldValue = null;

        while (fIt.hasNext()) {
            FieldValue fieldValue = (FieldValue) fIt.next();

            if (fieldValue.getField().getName().equals(name)) {
                matchingFieldValue = fieldValue;
            }
        }

        if (matchingFieldValue == null) {
            return null;
        }

        if (matchingFieldValue instanceof HprofInstanceObjectValue) {
            return ((HprofInstanceObjectValue) matchingFieldValue).getInstance();
        } else {
            return ((HprofInstanceValue) matchingFieldValue).getTypeValue();
        }
    }

    private int getInstanceFieldValuesOffset() {
        int idSize = dumpClass.getHprofBuffer().getIDSize();

        return 1 + idSize + 4 + idSize + 4;
    }

    private HprofHeap getHprof() {
        return dumpClass.getHprof();
    }
}
