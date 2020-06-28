package org.gridkit.jvmtool.stacktrace;

import java.util.List;

public interface StackFrameList extends Iterable<StackFrame>, GenericStackElementList<StackFrame> {

    /**
     * Stack has classical bottom up indexing.
     * Frame at index 0 is last call frame (tail), while last frame in list is root one.
     */
    public StackFrame frameAt(int n);

    public int depth();

    /**
     * Similar to {@link List#subList(int, int)}.
     *
     * @throws IndexOutOfBoundsException
     */
    public StackFrameList fragment(int from, int to);

    public StackFrame[] toArray();

    public boolean isEmpty();

}
