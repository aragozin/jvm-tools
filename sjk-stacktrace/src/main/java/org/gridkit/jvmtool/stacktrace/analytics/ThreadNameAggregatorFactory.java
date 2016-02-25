package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class ThreadNameAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new ThreadNameAggregatorFactory();
    }

    String name = null;
    
    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        name = threadInfo.threadName();
        if (name != null && name.length() > 32) {
            name = name.substring(0, 32);
        }
    }

    @Override
    public Object info() {
        return name;
    }
}
