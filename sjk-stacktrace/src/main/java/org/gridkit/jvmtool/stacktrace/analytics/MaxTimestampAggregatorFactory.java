package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class MaxTimestampAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new MaxTimestampAggregatorFactory();
    }

    long max = Long.MIN_VALUE;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        max = Math.max(max, threadInfo.timestamp());
    }

    @Override
    public Object info() {
        return max;
    }
}
