package org.gridkit.jvmtool.gcmon;

public interface MemoryPoolInfo {

    public String name();
    
    public boolean nonHeap();
    
    public Iterable<String> memoryManagers();
    
    public MemoryUsageBean peakUsage();

    public MemoryUsageBean currentUsage();

    public MemoryUsageBean collectionUsage();
}
