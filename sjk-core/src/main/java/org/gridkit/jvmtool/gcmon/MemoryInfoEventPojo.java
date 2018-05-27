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

import org.gridkit.jvmtool.event.SimpleCounterCollection;
import org.gridkit.jvmtool.event.SimpleTagCollection;

public class MemoryInfoEventPojo implements MemoryPoolInfoEvent {

    private long timestamp;

    private SimpleTagCollection tags = new SimpleTagCollection();
    private SimpleCounterCollection counters = new SimpleCounterCollection();

    @Override
    public long timestamp() {
        return timestamp;
    }
    
    public void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public SimpleCounterCollection counters() {
        return counters;
    }
    
    @Override
    public SimpleTagCollection tags() {
        return tags;
    }
    
    @Override
    public String name() {
        return tags.firstTagFor(MEM_POOL_NAME);
    }
    
    public void name(String value) {
        tags.put(MEM_POOL_NAME, value);
    }
    
    @Override
    public boolean nonHeap() {
        return tags.firstTagFor(MEM_POOL_NON_HEAP) != null;
    }
    
    public void nonHeap(boolean nonHeap) {
        if (nonHeap) {
            tags.put(MEM_POOL_NON_HEAP, "");
        }
    }
    
    @Override
    public Iterable<String> memoryManagers() {
        return tags.tagsFor(MEM_POOL_MEMORY_MANAGER);
    }
    
    @Override
    public MemoryUsageBean peakUsage() {
        return readMemUsage(MEM_POOL_MEMORY_PEAK);
    }
    
    public void peakUsage(MemoryUsageBean usage) {
        storeMemUsage(MEM_POOL_MEMORY_PEAK, usage);
    }
    
    @Override
    public MemoryUsageBean currentUsage() {
        return readMemUsage(MEM_POOL_MEMORY_USAGE);
    }
    
    public void currentUsage(MemoryUsageBean usage) {
        storeMemUsage(MEM_POOL_MEMORY_USAGE, usage);
    }
    
    @Override
    public MemoryUsageBean collectionUsage() {
        return readMemUsage(MEM_POOL_COLLECTION_USAGE);
    }
    
    public void collectionUsage(MemoryUsageBean usage) {
        storeMemUsage(MEM_POOL_COLLECTION_USAGE, usage);
    }
    
    private MemoryUsageBean readMemUsage(String name) {
        if (counters.getValue(name + "." + MEM_USAGE_INIT) >= 0) {
            return new MemoryUsageBean(
                    counters.getValue(name + "." + MEM_USAGE_INIT),
                    counters.getValue(name + "." + MEM_USAGE_USED),
                    counters.getValue(name + "." + MEM_USAGE_COMMITTED),
                    counters.getValue(name + "." + MEM_USAGE_MAX));
        }
        else {
            return null;
        }
    }

    private void storeMemUsage(String name, MemoryUsageBean bean) {
        counters.set(name + "." + MEM_USAGE_INIT, bean.init());
        counters.set(name + "." + MEM_USAGE_USED, bean.used());
        counters.set(name + "." + MEM_USAGE_COMMITTED, bean.committed());
        counters.set(name + "." + MEM_USAGE_MAX, bean.max());
    }
}
