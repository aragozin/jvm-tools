package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.jvmtool.stacktrace.ThreadCounters;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class AllocAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new AllocAggregatorFactory();
    }

    private Map<Long, ThreadTrack> info = new HashMap<Long, AllocAggregatorFactory.ThreadTrack>();

    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        if (threadInfo.counters().getValue(ThreadCounters.ALLOCATED_BYTES) != Long.MIN_VALUE) {
            ThreadTrack tt = info.get(threadInfo.threadId());
            if (tt == null) {
                tt = new ThreadTrack();
                tt.firstTimestamp = threadInfo.timestamp();
                tt.lastTimestamp = threadInfo.timestamp();
                tt.fisrtAlloc = threadInfo.counters().getValue(ThreadCounters.ALLOCATED_BYTES);
                tt.lastAlloc = threadInfo.counters().getValue(ThreadCounters.ALLOCATED_BYTES);
                info.put(threadInfo.threadId(), tt);
            }
            else {
                if (tt.firstTimestamp > threadInfo.timestamp()) {
                    tt.firstTimestamp = threadInfo.timestamp();
                    tt.fisrtAlloc = threadInfo.counters().getValue(ThreadCounters.ALLOCATED_BYTES);
                }
                if (tt.lastTimestamp < threadInfo.timestamp()) {
                    tt.lastTimestamp = threadInfo.timestamp();
                    tt.lastAlloc = threadInfo.counters().getValue(ThreadCounters.ALLOCATED_BYTES);
                }
            }
        }
    }

    @Override
    public Object info() {
        long totalAlloc = 0;
        long minTs = Long.MAX_VALUE;
        long maxTs = Long.MIN_VALUE;
        for(ThreadTrack tt: info.values()) {
            totalAlloc += tt.lastAlloc - tt.fisrtAlloc;
            minTs = Math.min(minTs, tt.firstTimestamp);
            maxTs = Math.max(maxTs, tt.lastTimestamp);
        }
        if (minTs == Long.MAX_VALUE || minTs == maxTs) {
            return Double.NaN;
        }
        else {
            return (((double)totalAlloc) / (maxTs - minTs)) * 1000d;
        }
    }

    private static class ThreadTrack {

        long firstTimestamp;
        long fisrtAlloc;

        long lastTimestamp;
        long lastAlloc;
    }
}
