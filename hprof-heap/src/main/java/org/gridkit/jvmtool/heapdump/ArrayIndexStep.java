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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;

class ArrayIndexStep extends PathStep {

    private final int index;

    public ArrayIndexStep(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
    
    @Override
    public Iterator<Instance> walk(Instance instance) {
        if (instance instanceof ObjectArrayInstance) {
            ObjectArrayInstance array = (ObjectArrayInstance) instance;
            if (index < 0) {
                return array.getValues().iterator();
            }
            else {
                if (array.getLength() > index) {
                    return Collections.singleton(array.getValues().get(index)).iterator();
                }
                else {
                    return Collections.<Instance>emptyList().iterator();
                }
            }
        }
        else {
            return Collections.<Instance>emptyList().iterator();
        }
    }

    @Override
    public Iterator<Move> track(Instance instance) {
        if (instance instanceof ObjectArrayInstance) {
            ObjectArrayInstance array = (ObjectArrayInstance) instance;
            if (index < 0) {
                List<Move> result = new ArrayList<Move>();
                int n = 0;
                for(Instance i: array.getValues()) {
                    result.add(new Move("[" + n + "]", i));
                    ++n;
                }
                return result.iterator();
            }
            else {
                if (array.getLength() > index) {
                    return Collections.singleton(new Move("[" + index + "]", array.getValues().get(index))).iterator();
                }
                else {
                    return Collections.<Move>emptyList().iterator();
                }
            }
        }
        else {
            return Collections.<Move>emptyList().iterator();
        }
    }

    @Override
    public String toString() {
        return index < 0 ? "[*]" : "[" + index + "]";
    }
}
