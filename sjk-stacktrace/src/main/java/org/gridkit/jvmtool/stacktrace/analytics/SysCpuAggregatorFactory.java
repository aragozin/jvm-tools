package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.jvmtool.stacktrace.ThreadCounters;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class SysCpuAggregatorFactory implements ThreadDumpAggregator, ThreadDumpAggregatorFactory {

    @Override
    public ThreadDumpAggregator newInstance() {
        return new SysCpuAggregatorFactory();
    }

    private Map<Long, ThreadTrack> info = new HashMap<Long, ThreadTrack>(); 
    
    @Override
    public void aggregate(ThreadSnapshot threadInfo) {
        if (cpuTime(threadInfo) >= 0 && userTime(threadInfo) >= 0) {
            ThreadTrack tt = info.get(threadInfo.threadId());
            if (tt == null) {
                tt = new ThreadTrack();
                tt.firstTimestamp = threadInfo.timestamp();
                tt.lastTimestamp = threadInfo.timestamp();
                tt.fisrtCPU = sysTime(threadInfo);
                tt.lastCPU = sysTime(threadInfo);
                info.put(threadInfo.threadId(), tt);
            }
            else {
                if (tt.firstTimestamp > threadInfo.timestamp()) {
                    tt.firstTimestamp = threadInfo.timestamp();
                    tt.fisrtCPU = sysTime(threadInfo);
                }
                if (tt.lastTimestamp < threadInfo.timestamp()) {
                    tt.lastTimestamp = threadInfo.timestamp();
                    tt.lastCPU = sysTime(threadInfo);
                }
            }
        }
    }

    private long sysTime(ThreadSnapshot threadInfo) {
    	return cpuTime(threadInfo) - userTime(threadInfo);
    }

    private long userTime(ThreadSnapshot threadInfo) {
		return threadInfo.counters().getValue(ThreadCounters.USER_TIME_MS);
	}

	private long cpuTime(ThreadSnapshot threadInfo) {
		return threadInfo.counters().getValue(ThreadCounters.CPU_TIME_MS);
	}

    @Override
    public Object info() {
        long totalCPU = 0;
        long minTs = Long.MAX_VALUE;
        long maxTs = Long.MIN_VALUE;
        for(ThreadTrack tt: info.values()) {
            totalCPU += tt.lastCPU - tt.fisrtCPU;
            minTs = Math.min(minTs, tt.firstTimestamp);
            maxTs = Math.max(maxTs, tt.lastTimestamp);
        }
        if (minTs == Long.MAX_VALUE || minTs == maxTs) {
            return Double.NaN;
        }
        else {
            return (((double)totalCPU) / (maxTs - minTs));
        }
    }
    
    private static class ThreadTrack {
        
        long firstTimestamp;
        long fisrtCPU;

        long lastTimestamp;
        long lastCPU;
    }
}
