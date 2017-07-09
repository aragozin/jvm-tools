/**
 * Copyright 2014 Alexey Ragozin
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
package org.gridkit.jvmtool.heapdump;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

public class PredicateStep extends PathStep {

    private PathStep[] path;
    private FieldStep lastStep;
    private String matcher;
    private boolean inverted;

    public PredicateStep(PathStep[] path, String matcher, boolean inverted) {
        if (path.length > 0 && path[path.length - 1] instanceof FieldStep) {
            this.path = Arrays.copyOf(path, path.length - 1);
            this.lastStep = (FieldStep) path[path.length - 1];
        }
        else {
            this.path = path;
        }
        this.matcher = matcher;
        this.inverted = inverted;
    }

    @Override
    public Iterator<Instance> walk(Instance instance) {
        if (instance != null && evaluate(instance)) {
            return Collections.singleton(instance).iterator();
        }
        else {
            return Collections.<Instance>emptyList().iterator();
        }
    }

    @Override
    public Iterator<Move> track(Instance instance) {
        if (instance != null && evaluate(instance)) {
            return Collections.singleton(new Move("", instance)).iterator();
        }
        else {
            return Collections.<Move>emptyList().iterator();
        }
    }

    protected boolean evaluate(Instance instance) {
        for(Instance i: HeapPath.collect(instance, path)) {
            if (lastStep != null) {
                String fname = lastStep.getFieldName();
                for(FieldValue fv: i.getFieldValues()) {
                    if ((fname == null && fv.getField().isStatic())
                            || (fname.equals(fv.getField().getName()))) {
                        Object obj;//HeapWalker.valueOf(i, "key")
                        if (fv instanceof ObjectFieldValue) {
                            obj = HeapWalker.valueOf(((ObjectFieldValue) fv).getInstance());
                        }
                        else {
                            // have to use this as private package API is used behind scene
                            obj = i.getValueOfField(fv.getField().getName());
                        }
                        if (!(obj instanceof Instance)) {
                            String str = String.valueOf(obj);
                            if (str.equals(matcher)) {
                                return !inverted;
                            }
                        }
                    }
                }
            }
            else {
                Object obj = HeapWalker.valueOf(i);
                if (!(obj instanceof Instance)) {
                    String str = String.valueOf(obj);
                    if (str.equals(matcher)) {
                        return !inverted;
                    }
                }
            }
        }
        return inverted;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(PathStep step: path) {
            sb.append(step).append(", ");
        }
        if (lastStep != null) {
            sb.append(lastStep).append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append(inverted ? "!=" : "=").append(matcher).append("]");
        return sb.toString();
    }
}
