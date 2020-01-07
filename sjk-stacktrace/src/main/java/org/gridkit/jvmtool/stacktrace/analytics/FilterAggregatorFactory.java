package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class FilterAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    private final ThreadSnapshotFilter filter;
    private final WeigthCalculator calc;
    private long total;
    private long matched;

    public FilterAggregatorFactory(ThreadSnapshotFilter filter, WeigthCalculator calc) {
        this.filter = filter;
        this.calc = calc;
    }

    @Override
    public ThreadDumpAggregator newInstance() {
        return new FilterAggregatorFactory(filter, calc);
    }

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        long w = calc.getWeigth(threadInfo);
        total += w;
        if (filter.evaluate(threadInfo)) {
            matched += w;
        }
    }

    @Override
    public Object info() {
        return ((double)matched) / total;
    }
}
