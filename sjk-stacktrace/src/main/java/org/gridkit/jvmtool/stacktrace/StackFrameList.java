package org.gridkit.jvmtool.stacktrace;

public interface StackFrameList extends Iterable<StackFrame> {

    /**
     * Stack has classical bottom up indexing.
     * Frame at index 0 is last call frame, while last frame in list is root one.
     */
    public StackFrame frameAt(int n);
    
    public int depth();
    
    public StackFrameList fragment(int from, int to);
    
    public StackFrame[] toArray();
    
    public boolean isEmpty();
    
}
