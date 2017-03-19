package org.gridkit.jvmtool.gcmon;

public interface MemoryPoolEventConsumer {

    public void consumeUsageEvent(MemoryPoolInfoEvent event);

    public void consumePeakEvent(MemoryPoolInfoEvent event);

    public void consumeCollectionUsageEvent(MemoryPoolInfoEvent event);
    
}
