package org.gridkit.jvmtool.heapdump;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

/** This a special step type handling java.util.HashMap */
class MapEntrySetStep extends PathStep {

    @Override
    public Iterator<Instance> walk(Instance instance) {
        Object t = instance.getValueOfField("table");
        if (t instanceof ObjectArrayInstance) {
            return new InstanceIterator(".table", (ObjectArrayInstance) t);
        }
        else {
            return Collections.<Instance>emptySet().iterator();
        }
    }

    @Override
    public Iterator<Move> track(Instance instance) {
        Object t = instance.getValueOfField("table");
        if (t instanceof ObjectArrayInstance) {
            return new MoveIterator(".table", (ObjectArrayInstance) t);
        }
        else {
            return Collections.<Move>emptySet().iterator();
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

    private static class EntryIterator {

        ObjectArrayInstance array;
        int nextIndex;
        Instance nextEntity;
        String pref;
        StringBuilder path;

        public EntryIterator(String pref, ObjectArrayInstance array) {
            this.array = array;
            this.pref = pref;
            this.path = new StringBuilder();
            seek();
        }

        public boolean hasNext() {
            return nextEntity != null;
        }

        public Instance nextInstance() {
            if (nextEntity == null) {
                throw new NoSuchElementException();
            }
            Instance e = nextEntity;
            seek();
            return e;
        }

        public Move nextMove() {
            if (nextEntity == null) {
                throw new NoSuchElementException();
            }
            Move e = new Move(path.toString(), nextEntity);
            seek();
            return e;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void seek() {
            if (nextEntity != null) {
                path.append(".next");
                nextEntity = getField(nextEntity, "next");
            }
            while(nextEntity == null && nextIndex < array.getLength()) {
                path.setLength(0);
                path.append(pref).append("[").append(nextIndex).append("]");
                nextEntity = array.getValues().get(nextIndex++);
            }
        }
    }

    private static class InstanceIterator extends EntryIterator implements Iterator<Instance> {

        public InstanceIterator(String pref, ObjectArrayInstance array) {
            super(pref, array);
        }

        @Override
        public Instance next() {
            return nextInstance();
        }
    }

    private static class MoveIterator extends EntryIterator implements Iterator<Move> {

        public MoveIterator(String pref, ObjectArrayInstance array) {
            super(pref, array);
        }

        @Override
        public Move next() {
            return nextMove();
        }
    }
}
