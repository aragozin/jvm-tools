package org.gridkit.jvmtool;

import org.gridkit.lab.jvm.perfdata.JStatData;
import org.gridkit.lab.jvm.perfdata.JStatData.TickCounter;

public class PerfCounterGcCpuUsageMonitor implements GcCpuUsageMonitor {

    TickCounter gc0;
    TickCounter gc1;
    
    public PerfCounterGcCpuUsageMonitor(long pid) {
        JStatData jd = JStatData.connect(pid);
        gc0 = (TickCounter) jd.getAllCounters().get("sun.gc.collector.0.time");
        gc1 = (TickCounter) jd.getAllCounters().get("sun.gc.collector.1.time");
    }
    
    
    @Override
    public long getYoungGcCpu() {
        return gc0 == null ? 0 : gc0.getNanos();
    }

    @Override
    public long getOldGcCpu() {
        return gc1 == null ? 0 : gc1.getNanos();
    }
}
