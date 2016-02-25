package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class FilterAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    private final ThreadSnapshotFilter filter;
    private long total;
    private long matched;
    
    public FilterAggregatorFactory(ThreadSnapshotFilter filter) {
        this.filter = filter;
    }

    @Override
    public ThreadDumpAggregator newInstance() {
        return new FilterAggregatorFactory(filter);
    }

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        total++;
        if (filter.evaluate(threadInfo)) {
            matched++; 
        }        
    }

    @Override
    public Object info() {
        return ((double)matched) / total;
    }
}