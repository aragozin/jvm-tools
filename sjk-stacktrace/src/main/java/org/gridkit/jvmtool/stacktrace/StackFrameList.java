package org.gridkit.jvmtool.stacktrace;

public interface StackFrameList extends Iterable<StackFrame> {

    public StackFrame frameAt(int n);
    
    public int depth();
    
    public StackFrameList fragment(int from, int to);
    
    public StackFrame[] toArray();
    
    public boolean isEmpty();
    
}
