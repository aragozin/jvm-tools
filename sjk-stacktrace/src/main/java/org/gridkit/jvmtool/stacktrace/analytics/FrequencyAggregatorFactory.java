package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class FrequencyAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new FrequencyAggregatorFactory();
    }

    long total;
    long minTs = Long.MAX_VALUE;
    long maxTs = Long.MIN_VALUE;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        ++total;
        minTs = Math.min(minTs, threadInfo.timestamp());
        maxTs = Math.max(maxTs, threadInfo.timestamp());
    }

    @Override
    public Object info() {
        if (total < 2) {
            return Double.NaN;
        }
        else {
            return 1000d * total / (maxTs - minTs);
        }
    }
}
