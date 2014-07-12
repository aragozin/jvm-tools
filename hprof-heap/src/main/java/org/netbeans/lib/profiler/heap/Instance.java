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

import java.util.List;


/**
 * This object represents one instance of java class.
 * @author Tomas Hurka
 */
public interface Instance {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * computes the list of instance field values. The order is: fields of this class followed by
     * super class, etc.
     * <br>
     * Speed: normal
     * @return list of {@link FieldValue} instance field values.
     */
    List /*<FieldValue>*/ getFieldValues();

    /**
     * returns <CODE>true</CODE> if this is instance of GC root.
     * <br>
     * Speed: normal for first invocation, fast for subsequent
     * @return <CODE>true</CODE> if this is instance of GC root, <CODE>false</CODE> otherwise.
     */
    boolean isGCRoot();

    /**
     * gets unique (in whole heap) ID of this {@link Instance}.
     * <br>
     * Speed: fast
     * @return ID of this {@link Instance}
     */
    long getInstanceId();

    /**
     * gets unique number of this {@link Instance} among all instances of the same Java Class.
     * Instances are numbered sequentially starting from 1.
     * <br>
     * Speed: fast
     * @return unique number of this {@link Instance}
     */
    int getInstanceNumber();

    /**
     * returns corresponding {@link JavaClass} for this instance.
     * <br>
     * Speed: fast
     * @return {@link JavaClass} of this instance.
     */
    JavaClass getJavaClass();

    /**
     * returns next {@link Instance} on the path to the nearest GC root.
     * <br>
     * Speed: first invocation is slow, all subsequent invocations are fast
     * @return next {@link Instance} on the path to the nearest GC root, itself if the instance is GC root,
     * <CODE>null</CODE> if path to the nearest GC root does not exist
     */
    Instance getNearestGCRootPointer();

    long getReachableSize();

    /**
     * returns the list of references to this instance. The references can be of two kinds.
     * The first one is from {@link ObjectFieldValue} and the second one if from {@link ArrayItemValue}
     * <br>
     * Speed: first invocation is slow, all subsequent invocations are fast
     * @return list of {@link Value} representing all references to this instance
     */
    List /*<Value>*/ getReferences();

    long getRetainedSize();

    /**
     * returns the size of the {@link Instance} in bytes. If the instance is not
     * {@link PrimitiveArrayInstance} or {@link ObjectArrayInstance} this size is
     * equal to <CODE>getJavaClass().getInstanceSize()</CODE>.
     * <br>
     * Speed: fast
     * @return size of this {@link Instance}
     */
    long getSize();

    /**
     * returns the list of static field values.
     * This is delegated to {@link JavaClass#getStaticFieldValues()}
     * <br>
     * Speed: normal
     * @return list of {@link FieldValue} static field values.
     */
    List /*<FieldValue>*/ getStaticFieldValues();

    /**
     * Returns a value object that reflects the specified field of the instance
     * represented by this {@link Instance} object. Fields are searched from the java.lang.Object.
     * The first field with the matching name is used.
     * The name parameter is a String that specifies the simple name of the desired field.
     * <br>
     * Speed: normal
     * @param name the name of the field
     * @return the value for the specified static field in this class.
     * If a field with the specified name is not found <CODE>null</CODE> is returned.
     * If the field.getType() is {@link Type} object {@link Instance} is returned as a field value,
     * for primitive types its corresponding object wrapper (Boolean, Integer, Float, etc.) is returned.
     */
    Object getValueOfField(String name);
}
