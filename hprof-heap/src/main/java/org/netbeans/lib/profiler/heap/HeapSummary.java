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
 * This is optional summary information. It contains summary heap data and
 * time of the heap dump.
 * @author Tomas Hurka
 */
public interface HeapSummary {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * the time when the memory heap dump occurred.
     * @return the time when the memory heap dump occurred in milliseconds since 0:00 GMT 1/1/1970
     */
    long getTime();

    /**
     * number of total bytes allocated on the heap during the run of JVM.
     * Returned only if this summary information is available in heap dump.
     * @return number of total allocated bytes on the heap during the run of JVM
     * or -1 if the information is not available in the heap dump.
     */
    long getTotalAllocatedBytes();

    /**
     * number of all instances allocated on the heap during the run of JVM.
     * Returned only if this summary information is available in heap dump.
     * @return number of instances allocated on the heap during the run of JVM
     * or -1 if the information is not available in the heap dump.
     */
    long getTotalAllocatedInstances();

    /**
     * number of total bytes allocated on the heap at the time of the heap dump.
     * If this summary information is not available in heap dump, it is computed
     * from the dump.
     * @return number of total allocated bytes in the heap
     */
    long getTotalLiveBytes();

    /**
     * total number of instances allocated on the heap at the time of the heap dump.
     * If this summary information is not available in heap dump, it is computed
     * from the dump.
     * @return number of total live instances in the heap
     */
    long getTotalLiveInstances();
}
