package org.gridkit.jvmtool.event;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingleEventReader<T extends Event> implements EventReader<T> {

    private final T event;
    private boolean done;

    public SingleEventReader(T event) {
        this.event = event;
    }

    @Override
    public <M extends Event> EventReader<M> morph(EventMorpher<T, M> morpher) {
        return MorphingEventReader.morph(this, morpher);
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return !done;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        done = true;
        return event;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T peekNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return event;
    }

    @Override
    public void dispose() {
        // do nothing
    }
}
