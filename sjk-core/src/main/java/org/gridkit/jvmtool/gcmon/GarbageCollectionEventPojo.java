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

import java.util.Iterator;

import org.gridkit.jvmtool.event.SimpleCounterCollection;
import org.gridkit.jvmtool.event.SimpleTagCollection;
import org.gridkit.jvmtool.jvmevents.JvmEvents;

public class GarbageCollectionEventPojo implements GarbageCollectionEvent {

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
    public long duration() {
        return counters().getValue("duration");
    }

    public void duration(long duration) {
        counters().set("duration", duration);
    }

    public void tag(String key, String tag) {
        tags.put(key, tag);
    }

    public void set(String counter, long value) {
        counters.set(counter, value);
    }

    @Override
    public String collectorName() {
        return firstTag(JvmEvents.GC_NAME);
    }

    public void collectorName(String name) {
        tags().put(JvmEvents.GC_NAME, name);
    }

    @Override
    public long collectionCount() {
        return counters.getValue(JvmEvents.GC_COUNT);
    }

    @Override
    public long collectionTotalTime() {
        return counters.getValue(JvmEvents.GC_TOTAL_TIME_MS);
    }

    @Override
    public Iterable<String> memorySpaces() {
        return tags.tagsFor(JvmEvents.GC_MEMORY_SPACES);
    }

    @Override
    public long memoryBefore(String space) {
        return counters.getValue(JvmEvents.memorySpaceBefore(space));
    }

    @Override
    public long memoryAfter(String space) {
        return counters.getValue(JvmEvents.memorySpaceUsed(space));
    }

    @Override
    public long memoryMax(String space) {
        return counters.getValue(JvmEvents.memorySpaceMax(space));
    }

    @Override
    public String memorySpaceName(String spaceId) {
        return firstTag(JvmEvents.memorySpaceName(spaceId));
    }

    private String firstTag(String key) {
        Iterator<String> it = tags.tagsFor(key).iterator();
        return it.hasNext() ? it.next() : null;
    }

    public void loadFrom(GarbageCollectionSummary gcevent) {
        collectorName(gcevent.collectorName());
        timestamp(gcevent.timestamp());
        duration(gcevent.duration());
        counters().set(JvmEvents.GC_COUNT, gcevent.collectionCount());
        counters().set(JvmEvents.GC_TOTAL_TIME_MS, gcevent.collectionTotalTime());
        for(String spaceId: gcevent.memorySpaces()) {
            tags().put(JvmEvents.GC_MEMORY_SPACES, spaceId);
            tags().put(JvmEvents.memorySpaceName(spaceId), gcevent.memorySpaceName(spaceId));
            counters().set(JvmEvents.memorySpaceBefore(spaceId), gcevent.memoryBefore(spaceId));
            counters().set(JvmEvents.memorySpaceUsed(spaceId), gcevent.memoryAfter(spaceId));
            counters().set(JvmEvents.memorySpaceMax(spaceId), gcevent.memoryMax(spaceId));
        }
    }
}
