package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class CountAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    final WeigthCalculator calc;
    long counter;

    public CountAggregatorFactory(WeigthCalculator calc) {
        this.calc = calc;
    }

    @Override
    public ThreadDumpAggregator newInstance() {
        return new CountAggregatorFactory(calc);
    }

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        long w = calc.getWeigth(threadInfo);
        counter += w;
    }

    @Override
    public Object info() {
        return counter;
    }
}
