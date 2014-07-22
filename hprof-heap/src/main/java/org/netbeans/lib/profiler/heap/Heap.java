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

import java.util.Collection;
import java.util.List;
import java.util.Properties;


/**
 * This is top-level interface representing one instance of heap dump.
 * @author Tomas Hurka
 */
public interface Heap {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * computes List of all {@link JavaClass} instances in this heap.
     * The classes are ordered according to the position in the dump file.
     * <br>
     * Speed: slow for the first time, subsequent invocations are fast.
     * @return list of all {@link JavaClass} in the heap.
     */
    List<JavaClass> getAllClasses();

    /**
     * @return iterable walking through all instances in the heap
     */
    Iterable<Instance> getAllInstances();

    /**
     * @return iterable walking through all instances in the heap, starting with given instanceID
     */
    Iterable<Instance> getAllInstances(long instanceID);

    /**
     * computes List of N biggest {@link JavaClass} instances in this heap.
     * The classes are ordered according to their retained size.
     * <br>
     * Speed: slow for the first time, subsequent invocations are normal.
     * @return list of N biggest {@link JavaClass} instances.
     */
    List<Instance> getBiggestObjectsByRetainedSize(int number);

    /**
     * returns {@link GCRoot} for {@link Instance}.
     * <br>
     * Speed: normal for first invocation, fast for subsequent
     * @param instance {@link Instance} whose associated {@link GCRoot} is to be returned.
     * @return {@link GCRoot} for corresponding instance or <CODE>null</CODE> if instance is not GC root.
     */
    GCRoot getGCRoot(Instance instance);

    /**
     * returns list of all GC roots.
     * <br>
     * Speed: normal for first invocation, fast for subsequent
     * @return list of {@link GCRoot} instances representing all GC roots.
     */
    Collection<GCRoot> getGCRoots();

    /**
     * computes {@link Instance} for instanceId.
     * <br>
     * Speed: fast
     * @param instanceId unique ID of {@link Instance}
     * @return return <CODE>null</CODE> if there no {@link Instance} with instanceId, otherwise
     * corresponding {@link Instance} is returned so that
     * <CODE>heap.getInstanceByID(instanceId).getInstanceId() == instanceId</CODE>
     */
    Instance getInstanceByID(long instanceId);

    /**
     * computes {@link JavaClass} for javaclassId.
     * <br>
     * Speed: fast
     * @param javaclassId unique ID of {@link JavaClass}
     * @return return <CODE>null</CODE> if there no java class with javaclassId, otherwise corresponding {@link JavaClass}
     * is returned so that <CODE>heap.getJavaClassByID(javaclassId).getJavaClassId() == javaclassId</CODE>
     */
    JavaClass getJavaClassByID(long javaclassId);

    /**
     * computes {@link JavaClass} for fully qualified name.
     * <br>
     * Speed: slow
     * @param fqn fully qualified name of the java class.
     * @return return <CODE>null</CODE> if there no class with fqn name, otherwise corresponding {@link JavaClass}
     * is returned so that <CODE>heap.getJavaClassByName(fqn).getName().equals(fqn)</CODE>
     */
    JavaClass getJavaClassByName(String fqn);

    /**
     * computes collection of {@link JavaClass} filtered by regular expression.
     * <br>
     * Speed: slow
     * @param regexp regular expression for java class name.
     * @return return collection of {@link JavaClass} instances, which names satisfy the regexp expression. This
     * collection is empty if no class matches the regular expression
     */
    Collection<JavaClass> getJavaClassesByRegExp(String regexp);

    /**
     * returns optional summary information of the heap.
     * If this information is not available in the dump,
     * some data (like number of instances) are computed
     * from the dump itself.
     * <br>
     * Speed: fast if the summary is available in dump, slow if
     * summary needs to be computed from dump.
     * @return {@link HeapSummary} of the heap
     */
    HeapSummary getSummary();

    /**
     * Determines the system properties of the {@link Heap}. It returns {@link Properties} with the same
     * content as if {@link System#getProperties()} was invoked in JVM, where this heap dump was taken.
     * <br>
     * Speed: slow
     * @return the system properties or <CODE>null</CODE> if the system properties cannot be computed from
     * this {@link Heap}
     */
    Properties getSystemProperties();
}
