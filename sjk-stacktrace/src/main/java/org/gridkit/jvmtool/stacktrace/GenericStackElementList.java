package org.gridkit.jvmtool.stacktrace;

public interface GenericStackElementList<T extends GenericStackElement> extends Iterable<T> {

    /**
     * Stack has classical bottom up indexing.
     * Frame at index 0 is last call frame, while last frame in list is root one.
     */
    public T frameAt(int n);

    public int depth();

    public GenericStackElementList<T> fragment(int from, int to);

    public T[] toArray();

    public boolean isEmpty();

}
