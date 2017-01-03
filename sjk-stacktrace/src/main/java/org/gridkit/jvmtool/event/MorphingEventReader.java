package org.gridkit.jvmtool.event;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MorphingEventReader<T extends Event> implements EventReader<T> {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <S extends Event, T extends Event> EventReader<T> morph(EventReader<S> source, EventMorpher<S, T> morpher) {
        return new MorphingEventReader(source, morpher);
    }

    private final EventReader<Event> nested;
    private final EventMorpher<Event, T> morpher;
    private T nextEvent;

    protected MorphingEventReader(EventReader<Event> source) {
        this(source, null);
    }

    public MorphingEventReader(EventReader<Event> source, EventMorpher<Event, T> morph) {
        this.nested = source;
        this.morpher = morph;
    }

    @Override
    public <M extends Event> EventReader<M> morph(EventMorpher<T, M> morpher) {
        return morph(this, morpher);
    }

    @Override
    public boolean hasNext() {
        while(nextEvent == null && nested.hasNext()) {
            Event e = nested.next();
            nextEvent = transform(e);
        }
        return nextEvent != null;
    }

    protected T transform(Event event) {
        return morpher.morph(event);
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T event = nextEvent;
        nextEvent = null;
        return event;
    }

    @Override
    public T peekNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T event = nextEvent;
        return event;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dispose() {
        nested.dispose();
    }
}
