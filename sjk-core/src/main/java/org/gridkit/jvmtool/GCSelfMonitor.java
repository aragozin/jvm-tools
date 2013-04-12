/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.jvmtool;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class GCSelfMonitor {
	
	private Map<String, CollectorTracker> trackers = new LinkedHashMap<String, CollectorTracker>();
	
	public GCSelfMonitor() {
		for(GarbageCollectorMXBean gcbean: getGarbageCollectorMXBeans()) {
			CollectorTracker tracker = new CollectorTracker(gcbean);
			trackers.put(gcbean.getName(), tracker);
		}
	}

	private List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
		return ManagementFactory.getGarbageCollectorMXBeans();
	}
	
	public void displayStats() {
		for(GarbageCollectorMXBean gcbean: getGarbageCollectorMXBeans()) {
			CollectorTracker tracker = trackers.get(gcbean.getName());
			if (tracker != null) {
				System.out.println(tracker.calculateStats(gcbean));
			}
		}
	}
	
	private static class CollectorTracker {
		
		private String name;
		private long initialCount;
		private long initialTime;
		
		public CollectorTracker(GarbageCollectorMXBean gcbean) {
			this.name = gcbean.getName();
			this.initialCount = gcbean.getCollectionCount();
			this.initialTime = gcbean.getCollectionTime();
		}
		
		public String calculateStats(GarbageCollectorMXBean gcbean) {
			long count = gcbean.getCollectionCount();
			long time = gcbean.getCollectionTime();
			
			while(gcbean.getCollectionCount() != count) {
				count = gcbean.getCollectionCount();
				time = gcbean.getCollectionTime();				
			}
			
			double avg = ((double)(time - initialTime)) / ((double)(count - initialCount));
			
			StringBuilder builder = new StringBuilder();
			builder.append(String.format("%s[ collections: %d | avg: %.4f secs | total: %.1f secs ]", name, count - initialCount, avg / 1000d, (time - initialTime) / 1000d));
			
			return builder.toString();
		}
	}
	
	public static void main(String[] args) {
		System.gc();
		for(GarbageCollectorMXBean gcbean: ManagementFactory.getGarbageCollectorMXBeans()) {
			System.out.println(gcbean.getName());
			System.out.println(" count: " + gcbean.getCollectionCount());
			System.out.println(" time: " + gcbean.getCollectionTime());
			System.out.println(" pools: " + Arrays.asList(gcbean.getMemoryPoolNames()));
		}
	}
}
