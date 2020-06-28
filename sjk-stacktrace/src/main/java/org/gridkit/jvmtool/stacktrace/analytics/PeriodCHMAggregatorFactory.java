package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class PeriodCHMAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new PeriodCHMAggregatorFactory();
    }

    long count = 0;
    double total = 0;
    double totalSquares = 0;
    long lastTimestamp = Long.MAX_VALUE;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        if (threadInfo.timestamp() > lastTimestamp) {
            double delta = 0.001d * (threadInfo.timestamp() - lastTimestamp);
            total += delta;
            totalSquares += delta * delta;
            count += 1;
        }
        lastTimestamp = threadInfo.timestamp();
    }

    @Override
    public Object info() {
        if (count > 0) {
            return totalSquares / total;
        }
        else {
            return Double.NaN;
        }
    }
}
