package org.gridkit.jvmtool;

public interface GcCpuUsageMonitor {

    public long getYoungGcCpu();

    public long getOldGcCpu();
    
}
