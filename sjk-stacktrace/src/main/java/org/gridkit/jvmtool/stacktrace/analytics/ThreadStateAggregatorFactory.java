package org.gridkit.jvmtool.stacktrace.analytics;

import java.lang.Thread.State;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class ThreadStateAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    private final State state;

    public ThreadStateAggregatorFactory(State state) {
        this.state = state;
    }

    @Override
    public ThreadDumpAggregator newInstance() {
        return new ThreadStateAggregatorFactory(state);
    }

    long total;
    long matched;

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        ++total;
        if (threadInfo.threadState() == state) {
            ++matched;
        }
    }

    @Override
    public Object info() {
        return ((double)matched) / total;
    }
}
