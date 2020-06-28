package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class MinTimestampAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new MinTimestampAggregatorFactory();
    }

    long min = Long.MAX_VALUE;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        min = Math.min(min, threadInfo.timestamp());
    }

    @Override
    public Object info() {
        return min;
    }
}
