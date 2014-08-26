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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

class FieldStep extends PathStep {

    private final String fieldName;

    FieldStep(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Iterator<Instance> walk(Instance instance) {
        List<Instance> result = new ArrayList<Instance>();
        for(FieldValue fv: instance.getFieldValues()) {
            if (fieldName == null || fieldName.equals(fv.getField().getName())) {
                if (fv instanceof ObjectFieldValue) {
                    result.add(((ObjectFieldValue) fv).getInstance());
                }
            }
        }

        return result.iterator();
    }

    @Override
    public Iterator<Move> track(Instance instance) {
        List<Move> result = new ArrayList<Move>();
        for(FieldValue fv: instance.getFieldValues()) {
            if (fieldName == null || fieldName.equals(fv.getField().getName())) {
                if (fv instanceof ObjectFieldValue) {
                    result.add(new Move("." + fv.getField().getName(), ((ObjectFieldValue) fv).getInstance()));
                }
            }
        }

        return result.iterator();
    }

    @Override
    public String toString() {
        return fieldName == null ? "*" :  fieldName;
    }
}
