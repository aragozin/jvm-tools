package org.gridkit.jvmtool.stacktrace;

import java.util.Collection;

public class StackFrameArray extends AbstractStackFrameArray {

    final StackFrame[] array;
    final int from;
    final int to;

    public StackFrameArray(Collection<StackFrame> frames) {
    	this(frames.toArray(new StackFrame[frames.size()]));
    }
    
    public StackFrameArray(StackFrame[] array) {
        this(array, 0, array.length);
    }

    public StackFrameArray(StackFrame[] array, int from, int to) {
        this.array = array;
        this.from = from;
        this.to = to;
    }

    protected StackFrame[] array() {
        return array;
    }
    
    protected int from() {
        return from;
    }

    protected int to() {
        return to;
    }

    @Override
    public boolean isEmpty() {
        return from == to;
    }
}
