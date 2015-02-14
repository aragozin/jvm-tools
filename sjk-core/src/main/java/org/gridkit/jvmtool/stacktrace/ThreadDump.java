package org.gridkit.jvmtool.stacktrace;

import java.lang.Thread.State;
import java.util.ArrayList;

public class ThreadDump extends ArrayList<StackFrame> {

    private State state;

    public State getThreadState() {
        return state;
    }


}
