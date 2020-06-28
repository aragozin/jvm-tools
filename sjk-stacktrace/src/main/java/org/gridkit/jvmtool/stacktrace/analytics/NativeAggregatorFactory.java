package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class NativeAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new NativeAggregatorFactory();
    }

    long total;
    long matched;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        ++total;
        if (threadInfo.stackTrace().isEmpty() || threadInfo.stackTrace().frameAt(0).isNative()) {
            ++matched;
        }

    }

    @Override
    public Object info() {
        if (total > 0) {
            return ((double)matched) / total;
        }
        else {
            return Double.NaN;
        }
    }
}
