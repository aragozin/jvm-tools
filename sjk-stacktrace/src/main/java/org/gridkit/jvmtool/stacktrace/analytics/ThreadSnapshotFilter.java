package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public interface ThreadSnapshotFilter {

    public boolean evaluate(ThreadSnapshot snapshot);

}
