package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class ThreadNameAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    int lengthLimit;

    public ThreadNameAggregatorFactory(int lengthLimit) {
        this.lengthLimit = lengthLimit;
    }

    @Override
    public ThreadDumpAggregator newInstance() {
        return new ThreadNameAggregatorFactory(lengthLimit);
    }

    String name = null;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        name = threadInfo.threadName();
        if (name != null && name.length() > lengthLimit) {
            name = name.substring(0, lengthLimit);
        }
    }

    @Override
    public Object info() {
        return name;
    }
}
