package org.gridkit.jvmtool.stacktrace;

public class StackFrameArray extends AbstractStackFrameArray {

    final StackFrame[] array;
    final int from;
    final int to;
    
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
}
