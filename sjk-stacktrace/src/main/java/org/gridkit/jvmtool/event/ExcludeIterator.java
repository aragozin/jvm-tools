package org.gridkit.jvmtool.event;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Subtract one {@link Iterator} from another.
 * <br/>
 * Each of nested {@link Iterator}s MUST produce ordered, duplicate free
 * sequence of values.
 * <br/>
 * Result would be ordered, duplicate free sequence of values present
 * in iterator A, but not present in V.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ExcludeIterator<T> implements Iterator<T> {

    @SuppressWarnings("rawtypes")
    private static final Comparator NATURAL = new NaturalComaprator();

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> exclude(Iterator<T> a, Iterator<T> b) {
        return exclude(a, b, NATURAL);
    }

    public static <T> Iterator<T> exclude(Iterator<T> a, Iterator<T> b, Comparator<T> cmp) {
        return new ExcludeIterator<T>(a, b, cmp);
    }

    @SuppressWarnings("unchecked")
    public static <T> Iterable<T> exclude(final Iterable<T> a, final Iterable<T> b) {
        return exclude(a, b, NATURAL);
    }

    public static <T> Iterable<T> exclude(final Iterable<T> a, final Iterable<T> b, final Comparator<T> cmp) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return exclude(a.iterator(), b.iterator(), cmp);
            }
        };
    }

    private final Iterator<T> a;
    private final Iterator<T> b;
    private final Comparator<T> comparator;
    private T peekA;
    private T peekB;

    @SuppressWarnings("unchecked")
    public ExcludeIterator(Iterator<T> a, Iterator<T> b, Comparator<T> cmp) {
        this.a = a;
        this.b = b;
        this.comparator = cmp == null ? NATURAL : cmp;
        peekA = a.hasNext() ? next(a) : null;
        peekB = b.hasNext() ? next(b) : null;
        seek();
    }

    private T next(Iterator<T> it) {
        T v = it.next();
        if (v == null) {
            throw new NullPointerException("null element is not allowed");
        }
        return v;
    }

    private void seek() {
        while(peekA != null && peekB != null) {
            int c = comparator.compare(peekA, peekB);
            if (c > 0) {
                peekB = b.hasNext() ? b.next() : null;
                continue;
            }
            else if (c == 0) {
                peekA = a.hasNext() ? a.next() : null;
                peekB = b.hasNext() ? b.next() : null;
                continue;
            }
            else {
                break;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return peekA != null;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T result = peekA;
        peekA = a.hasNext() ? a.next() : null;
        seek();
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private static class NaturalComaprator implements Comparator<Comparable<Object>> {

        @Override
        public int compare(Comparable<Object> o1, Comparable<Object> o2) {
            return o1.compareTo(o2);
        }
    }
}
