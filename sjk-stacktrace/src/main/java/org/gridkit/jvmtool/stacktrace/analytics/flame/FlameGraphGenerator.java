/**
 * Copyright 2016 Alexey Ragozin
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
package org.gridkit.jvmtool.stacktrace.analytics.flame;

import java.util.Comparator;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.analytics.WeigthCalculator;

/**
 * Specialization of {@link GenericFlameGraphGenerator} working
 * with Java stack traces.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class FlameGraphGenerator extends GenericFlameGraphGenerator {

    private static final FrameComparator FRAME_COMPARATOR = new FrameComparator();

    public FlameGraphGenerator() {
        super();
    }

    public FlameGraphGenerator(WeigthCalculator calculator) {
        super(calculator);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Comparator<GenericStackElement> comparator() {
        return (Comparator)FRAME_COMPARATOR;
    }

    @Override
    protected String describe(Node node) {
        StackFrame frame = (StackFrame) node.path[node.path.length - 1];
        String line = frame.getClassName() + "." + frame.getMethodName();
        line = line.replace((CharSequence)"<", "&lt;");
        line = line.replace((CharSequence)">", "&gt;");
        return line;
    }

    private static class FrameComparator implements Comparator<StackFrame> {

        @Override
        public int compare(StackFrame o1, StackFrame o2) {
            int n = compare(o1.getClassName(), o2.getClassName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getLineNumber(), o2.getLineNumber());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getMethodName(), o2.getMethodName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getSourceFile(), o2.getSourceFile());
            return 0;
        }

        private int compare(int n1, int n2) {
            return Long.signum(((long)n1) - ((long)n2));
        }

        private int compare(String str1, String str2) {
            if (str1 == str2) {
                return 0;
            }
            else if (str1 == null) {
                return -1;
            }
            else if (str2 == null) {
                return 1;
            }
            return str1.compareTo(str2);
        }
    }
}
