/**
 * Copyright 2017 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
