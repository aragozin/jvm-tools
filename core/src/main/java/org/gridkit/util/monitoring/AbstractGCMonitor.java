package org.gridkit.util.monitoring;

import java.lang.management.GarbageCollectorMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractGCMonitor {

	private Map<String, CollectorTracker> trackers = new LinkedHashMap<String, CollectorTracker>();

	protected abstract List<GarbageCollectorMXBean> getGarbageCollectorMXBeans();

	protected void initSnapshot() {
		for(GarbageCollectorMXBean gcbean: getGarbageCollectorMXBeans()) {
			CollectorTracker tracker = new CollectorTracker(gcbean);
			trackers.put(gcbean.getName(), tracker);
		}
	}

	public void displayStats() {
		for(GarbageCollectorMXBean gcbean: getGarbageCollectorMXBeans()) {
			CollectorTracker tracker = trackers.get(gcbean.getName());
			if (tracker != null) {
				System.out.println(tracker.calculateStats(gcbean));
			}
		}
	}

	protected static class CollectorTracker {
			
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
}
