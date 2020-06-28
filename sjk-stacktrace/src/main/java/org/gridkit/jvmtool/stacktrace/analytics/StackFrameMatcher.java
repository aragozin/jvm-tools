package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.StackFrame;

public interface StackFrameMatcher {

    public boolean evaluate(StackFrame frame);

}
