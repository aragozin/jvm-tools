package org.gridkit.jvmtool.codec.stacktrace;

import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.stacktrace.StackFrameList;

public interface ThreadTraceEvent extends Event {

    StackFrameList stackTrace();
}
