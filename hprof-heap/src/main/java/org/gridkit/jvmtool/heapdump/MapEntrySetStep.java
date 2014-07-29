package org.gridkit.jvmtool.heapdump;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

/** This a special step type handling java.util.HashMap */
class MapEntrySetStep extends PathStep {

    @Override
    public Iterator<Instance> walk(Instance instance) {
        if (instance instanceof ObjectArrayInstance) {
            return new EntryIterator((ObjectArrayInstance) instance);
        }
        else {
            return null;
        }
    }

    public String toString() {
        return "?entrySet";
    }

    private static Instance getField(Instance i, String field) {
        for(FieldValue fv: i.getFieldValues()) {
            if (fv instanceof ObjectFieldValue && field.equals(fv.getField().getName())) {
                return ((ObjectFieldValue)fv).getInstance();
            }
        }
        return null;
    }

    private static class EntryIterator implements Iterator<Instance> {

        ObjectArrayInstance array;
        int nextIndex;
        Instance nextEntity;

        public EntryIterator(ObjectArrayInstance array) {
            this.array = array;
            seek();
        }

        @Override
        public boolean hasNext() {
            return nextEntity != null;
        }

        @Override
        public Instance next() {
            if (nextEntity == null) {
                throw new NoSuchElementException();
            }
            Instance e = nextEntity;
            seek();
            return e;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void seek() {
            if (nextEntity != null) {
                nextEntity = getField(nextEntity, "next");
            }
            while(nextEntity == null && nextIndex < array.getLength()) {
                nextEntity = array.getValues().get(nextIndex++);
            }
        }
    }
}
