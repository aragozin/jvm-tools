package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.Map;
import java.util.TreeMap;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public class ThreadSplitAggregator {

    private ThreadDumpAggregatorFactory[] aggregators;

    private Map<Long, ThreadTrack> data = new TreeMap<Long, ThreadTrack>();

    public ThreadSplitAggregator(ThreadDumpAggregatorFactory... aggregators) {
        this.aggregators = aggregators;
    }

    public void feed(ThreadSnapshot threadDump) {
        ThreadTrack tt = track(threadDump.threadId());
        process(tt, threadDump);
    }

    private ThreadTrack track(long threadId) {
        ThreadTrack tt = data.get(threadId);
        if (tt == null) {
            tt = new ThreadTrack();
            tt.aggregations = new ThreadDumpAggregator[aggregators.length];
            for(int i = 0; i != tt.aggregations.length; ++i) {
                tt.aggregations[i] = aggregators[i].newInstance();
            }
            data.put(threadId, tt);
        }
        return tt;
    }

    private void process(ThreadTrack tt, ThreadSnapshot threadDump) {
        for(ThreadDumpAggregator tda: tt.aggregations) {
            tda.aggregate(threadDump);
        }
        tt.threadName = threadDump.threadName();
    }

    public Object[][] report() {
        Object[][] result = new Object[data.size()][];
        int n = 0;
        for(long threadId: data.keySet()) {
            result[n] = new Object[aggregators.length + 2];
            ThreadTrack tt = data.get(threadId);
            result[n][0] = tt.threadName;
            result[n][1] = threadId;
            for(int i = 0; i !=  aggregators.length; ++i) {
                result[n][2 + i] = tt.aggregations[i].info();
            }
            ++n;
        }

        return result;
    }

    private static class ThreadTrack {

        String threadName;
        ThreadDumpAggregator[] aggregations;

    }
}
