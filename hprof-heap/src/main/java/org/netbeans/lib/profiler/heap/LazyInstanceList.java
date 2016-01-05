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
package org.netbeans.lib.profiler.heap;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lazy list implementation. Heap is scanned lazily, returning instances
 * or required type.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class LazyInstanceList extends AbstractList<Instance> {

    private final Heap heap;
    private final JavaClass jclass;
    private Iterator<Instance> cursor;
    private int currentIndex = -1;
    private Instance currentInstance = null;
    private int size = -1;

    public LazyInstanceList(Heap heap, JavaClass jclass) {
        this.heap = heap;
        this.jclass = jclass;
    }

    public LazyInstanceList(Heap heap, JavaClass jclass, int size) {
        this.heap = heap;
        this.jclass = jclass;
        this.size = size == 0 ? -1 : size;
    }

    @Override
    public Instance get(int index) {
        if (currentIndex == index) {
            return currentInstance;
        }
        if (currentIndex > index || cursor == null) {
            currentIndex = -1;
            cursor = new FilteredIterator(jclass, heap.getAllInstances().iterator());
        }
        while(cursor.hasNext() && currentIndex < index) {
            ++currentIndex;
            currentInstance = cursor.next();
        }
        if (currentIndex != index) {
            throw new IndexOutOfBoundsException(index + " > " + currentIndex);
        }
        return currentInstance;
    }

    @Override
    public Iterator<Instance> iterator() {
        return new FilteredIterator(jclass, heap.getAllInstances().iterator());
    }

    @Override
    public int size() {
        if (size >= 0) {
            return size;
        }
        else {
            int n = 0;
            Iterator<Instance> it = iterator();
            while(it.hasNext()) {
                it.next();
                ++n;
            }
            size = n;
            return size;
        }
    }

    private static class FilteredIterator implements Iterator<Instance> {

        private final JavaClass type;
        private final Iterator<Instance> nested;
        private Instance instance;

        public FilteredIterator(JavaClass type, Iterator<Instance> nested) {
            this.type = type;
            this.nested = nested;
            seek();
        }

        void seek() {
            while(nested.hasNext()) {
                Instance n = nested.next();
                if (n.getJavaClass() == type) {
                    instance = n;
                    return;
                }
            }
            instance = null;
        }

        @Override
        public boolean hasNext() {
            return instance != null;
        }

        @Override
        public Instance next() {
            if (instance == null) {
                throw new NoSuchElementException();
            }
            else {
                Instance n = instance;
                seek();
                return n;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
