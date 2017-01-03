package org.gridkit.jvmtool.event;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

public abstract class ChainedEventReader<T extends Event> implements EventReader<T> {

    public static <T extends Event> EventReader<T> chain(final EventReader<T>... readers) {
        return new ChainedEventReader<T>() {
            int next;

            @Override
            protected EventReader<T> produceNext() {
                return next < readers.length ? readers[next++] : null;
            }
        };
    }

    public static <T extends Event> EventReader<T> chain(final EventReader<T> reader, final Callable<T> producer) {
        return new ChainedEventReader<T>() {

            boolean first;
            boolean done;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected EventReader<T> produceNext() {
                if (!first) {
                    first = true;
                    return reader;
                }
                if (done) {
                    return null;
                }
                else {
                    done = true;
                    try {
                        return (EventReader<T>) producer.call();
                    } catch (Exception e) {
                        return new SingleEventReader(new SimpleErrorEvent(e));
                    }
                }
            }
        };
    }

    private boolean init;
    private EventReader<T> current;

    public ChainedEventReader() {
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
        if (!init) {
            init = true;
            current = produceNext();
        }
        if (current == null) {
            return false;
        }
        while(true) {
            if (current.hasNext()) {
                return true;
            }
            else {
                current.dispose();
                current = produceNext();
                if (current == null) {
                    return false;
                }
            }
        }
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return current.next();
    }

    @Override
    public T peekNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return current.peekNext();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

//    @Override
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    public <X extends Event> EventReader<X> filterByClass(Class<X> typeFilter) {
//        this.typeFilter = (Class)typeFilter;
//        if (current != null) {
//            current = (EventReader)current.filterByClass(typeFilter);
//        }
//        return (EventReader)this;
//    }
//
    @Override
    public void dispose() {
        if (current != null) {
            current.dispose();
        }
    }

    protected abstract EventReader<T> produceNext();
}
