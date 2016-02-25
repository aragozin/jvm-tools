package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class CountAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new CountAggregatorFactory();
    }

    long counter;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        ++counter;
    }

    @Override
    public Object info() {
        return counter;
    }
}
