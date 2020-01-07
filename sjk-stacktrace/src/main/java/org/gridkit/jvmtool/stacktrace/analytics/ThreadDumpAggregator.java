package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public interface ThreadDumpAggregator {

    public void aggregate(ThreadSnapshot threadInfo);

    public Object info();

}
