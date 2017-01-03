package org.gridkit.jvmtool.codec.stacktrace;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public interface ThreadSnapshotEvent extends CommonEvent, ThreadTraceEvent, ThreadSnapshot {

}
