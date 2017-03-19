package org.gridkit.jvmtool.gcmon;

import org.gridkit.jvmtool.event.CommonEvent;

public interface MemoryPoolInfoEvent extends MemoryPoolInfo, CommonEvent {

    public static final String MEM_POOL_NAME = "jvm.memory-pool.name";

    public static final String MEM_POOL_NON_HEAP = "jvm.memory-pool.nonHeap";
    public static final String MEM_POOL_MEMORY_MANAGER = "jvm.memory-pool.memoryManager";

    public static final String MEM_POOL_MEMORY_USAGE = "jvm.memory-pool.memoryUsage";
    public static final String MEM_POOL_MEMORY_PEAK = "jvm.memory-pool.memoryPeak";
    public static final String MEM_POOL_COLLECTION_USAGE = "jvm.memory-pool.collectionUsage";
    
    public static final String MEM_USAGE_INIT = "init";
    public static final String MEM_USAGE_USED = "used";
    public static final String MEM_USAGE_COMMITTED = "committed";
    public static final String MEM_USAGE_MAX = "max";
}
