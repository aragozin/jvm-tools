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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.gridkit.util.formating.Formats;

/**
 * Thread CPU tracker.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class MBeanCpuUsageReporter {

	private static final ObjectName THREADING_MBEAN = name("java.lang:type=Threading");
	private static ObjectName name(String name) {
		try {
			return new ObjectName(name);
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(e);
		}
	}

	private MBeanServerConnection mserver;

	private long lastTimestamp;
	private long lastProcessCpuTime;
	private long lastYougGcCpuTime;
	private long lastOldGcCpuTime;
	private BigInteger lastCummulativeCpuTime;
	private BigInteger lastCummulativeUserTime;
	private BigInteger lastCummulativeAllocatedAmount;
	
	private Map<Long, CompositeData> threadDump = new HashMap<Long, CompositeData>();
	private Map<Long, ThreadNote> notes = new HashMap<Long, MBeanCpuUsageReporter.ThreadNote>();
	
	private List<Comparator<ThreadLine>> comparators = new ArrayList<Comparator<ThreadLine>>();
	
	private int topLimit = Integer.MAX_VALUE;
	
	private Pattern filter;
	
	private boolean threadAllocatedMemoryEnabled;
	
	private GcCpuUsageMonitor gcMon;
	
	public MBeanCpuUsageReporter(MBeanServerConnection mserver) {
		this.mserver = mserver;
		
		threadAllocatedMemoryEnabled = getThreadingMBeanCapability("ThreadAllocatedMemoryEnabled");
		
		lastTimestamp = System.nanoTime();
		lastProcessCpuTime = getProcessCpuTime();
	}
	
	public void setGcCpuUsageMonitor(GcCpuUsageMonitor gcMon) {
	    this.gcMon = gcMon;
	}
	
	private boolean getThreadingMBeanCapability(String attrName) {
		try {
			Object val = mserver.getAttribute(THREADING_MBEAN, attrName);
			return Boolean.TRUE.equals(val);
		}
		catch(Exception e) {
			return false;
		}
	}

	public void sortByThreadName() {
		comparators.add(0, new ThreadNameComparator());
	}

	public void sortByUserCpu() {
		comparators.add(0, new UserTimeComparator());
	}

	public void sortBySysCpu() {
		comparators.add(0, new SysTimeComparator());
	}

	public void sortByTotalCpu() {
		comparators.add(0, new CpuTimeComparator());
	}

	public void sortByAllocRate() {
		comparators.add(0, new AllocRateComparator());
	}
	
	public void setTopLimit(int n) {
		topLimit = n;
	}

	public void setThreadFilter(Pattern regEx) {
		filter = regEx;
	}
	
	public String report() {

		dumpThreads();
		
		StringBuilder sb = new StringBuilder();
		
		long currentTime = System.nanoTime();
		long timeSplit = currentTime - lastTimestamp;
		long currentCpuTime = getProcessCpuTime();
		long currentYoungGcCpuTime = gcMon == null ? 0 : gcMon.getYoungGcCpu();
		long currentOldGcCpuTime = gcMon == null ? 0 : gcMon.getOldGcCpu();
		
		Map<Long,ThreadNote> newNotes = new HashMap<Long, ThreadNote>();
		
		BigInteger totalCpu = BigInteger.valueOf(0);
		BigInteger totalUser = BigInteger.valueOf(0);
		BigInteger totalAlloc = BigInteger.valueOf(0);
		
		List<ThreadLine> table = new ArrayList<ThreadLine>();
		
		for(long tid: getAllThreadIds()) {
			
			String threadName = getThreadName(tid);
			if (filter != null && !filter.matcher(threadName).matches()) {
				continue;
			}
			
			ThreadNote lastNote = notes.get(tid);
			ThreadNote newNote = new ThreadNote();
			newNote.lastCpuTime = getThreadCpuTime(tid);
			newNote.lastUserTime = getThreadUserTime(tid);
			newNote.lastAllocatedBytes = getThreadAllocatedBytes(tid);
			
			newNotes.put(tid, newNote);
			
			totalCpu = totalCpu.add(BigInteger.valueOf(newNote.lastCpuTime));
			totalUser = totalUser.add(BigInteger.valueOf(newNote.lastUserTime));
			totalAlloc = totalAlloc.add(BigInteger.valueOf(newNote.lastAllocatedBytes));
			
			if (lastNote != null) {

				double cpuT = ((double)(newNote.lastCpuTime - lastNote.lastCpuTime)) / timeSplit;
				double userT = ((double)(newNote.lastUserTime - lastNote.lastUserTime)) / timeSplit;
				double allocRate = ((double)(newNote.lastAllocatedBytes - lastNote.lastAllocatedBytes)) * TimeUnit.SECONDS.toNanos(1) / timeSplit;

				table.add(new ThreadLine(tid, 100 * userT, 100 * (cpuT - userT), allocRate, getThreadName(tid)));
			}
		}
		
		if (table.size() >0) {				

			for(Comparator<ThreadLine> cmp: comparators) {
				Collections.sort(table, cmp);
			}

			if (table.size() > topLimit) {
				table = table.subList(0, topLimit);
			}
			
			double processT = ((double)(currentCpuTime - lastProcessCpuTime)) / timeSplit;
			double cpuT = ((double)(totalCpu.subtract(lastCummulativeCpuTime).longValue())) / timeSplit;
			double userT = ((double)(totalUser.subtract(lastCummulativeUserTime).longValue())) / timeSplit;
			double allocRate = ((double)(totalAlloc.subtract(lastCummulativeAllocatedAmount).longValue())) * TimeUnit.SECONDS.toNanos(1) / timeSplit;

			double youngGcT = ((double)currentYoungGcCpuTime - lastYougGcCpuTime) / timeSplit;
			double oldGcT = ((double)currentOldGcCpuTime - lastOldGcCpuTime) / timeSplit;
			
			sb.append(Formats.toDatestamp(System.currentTimeMillis()));
			sb.append(String.format(" Process summary \n  process cpu=%.2f%%\n  application cpu=%.2f%% (user=%.2f%% sys=%.2f%%)\n  other: cpu=%.2f%% \n", 100 * processT, 100 * cpuT, 100 * userT, 100 * (cpuT - userT), 100 * (processT - cpuT)));
			if (currentYoungGcCpuTime > 0) {
			    sb.append(String.format("  GC cpu=%.2f%% (young=%.2f%%, old=%.2f%%)\n", 100 * (youngGcT + oldGcT), 100 * youngGcT, 100 * oldGcT));
			}
			if (threadAllocatedMemoryEnabled) {
				sb.append(String.format("  heap allocation rate %sb/s\n", Formats.toMemorySize((long) allocRate)));
			}
			for(ThreadLine line: table) {
				sb.append(format(line)).append('\n');
			}
			sb.append("\n");			
		}
		
		lastTimestamp = currentTime;
		notes = newNotes;
		lastCummulativeCpuTime = totalCpu;
		lastCummulativeUserTime = totalUser;
		lastCummulativeAllocatedAmount = totalAlloc;
		lastProcessCpuTime = currentCpuTime;
		lastYougGcCpuTime = currentYoungGcCpuTime;
		lastOldGcCpuTime = currentOldGcCpuTime;
		
		return sb.toString();
	}

	private Object format(ThreadLine line) {
		if (threadAllocatedMemoryEnabled) {
			return String.format("[%06d] user=%5.2f%% sys=%5.2f%% alloc=%6sb/s - %s", line.id, line.userT, (line.sysT), Formats.toMemorySize((long)line.allocRate), line.name);
		}
		else {
			return String.format("[%06d] user=%5.2f%% sys=%5.2f%% - %s", line.id, line.userT, (line.sysT), line.name);
		}
	}

	private void dumpThreads() {
		try {
			ObjectName bean = THREADING_MBEAN;
			CompositeData[] ti = (CompositeData[]) mserver.invoke(bean, "dumpAllThreads", new Object[]{Boolean.TRUE, Boolean.TRUE}, new String[]{"boolean", "boolean"});
			threadDump.clear();
			for(CompositeData t:ti) {
				threadDump.put((Long) t.get("threadId"), t);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getThreadName(long tid) {
		try {
			CompositeData info = threadDump.get(tid);
			return (String) info.get("threadName");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Collection<Long> getAllThreadIds() {
		return new TreeSet<Long>(threadDump.keySet());
	}

	private long getThreadCpuTime(long tid) {
		try {
			ObjectName bean = THREADING_MBEAN;
			long time = (Long) mserver.invoke(bean, "getThreadCpuTime", new Object[]{tid}, new String[]{"long"});
			return time;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private long getThreadUserTime(long tid) {
		try {
			ObjectName bean = THREADING_MBEAN;
			long time = (Long) mserver.invoke(bean, "getThreadUserTime", new Object[]{tid}, new String[]{"long"});
			return time;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private long getThreadAllocatedBytes(long tid) {
		if (threadAllocatedMemoryEnabled) {
			try {
				ObjectName bean = THREADING_MBEAN;
				long bytes = (Long) mserver.invoke(bean, "getThreadAllocatedBytes", new Object[]{tid}, new String[]{"long"});
				return bytes;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		else {
			return -1;
		}
	}

	private long getProcessCpuTime() {
		try {
			ObjectName bean = new ObjectName("java.lang:type=OperatingSystem");
			return (Long) mserver.getAttribute(bean, "ProcessCpuTime");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class ThreadNote {
		
		private long lastCpuTime;
		private long lastUserTime;
		private long lastAllocatedBytes;
		
	}
	
	private static class ThreadLine {
		
		long id;
		double userT;
		double sysT;
		double allocRate;
		String name;
		
		public ThreadLine(long id, double userT, double sysT, double allocRate, String name) {
			this.id = id;
			this.userT = userT;
			this.sysT = sysT;
			this.allocRate = allocRate;
			this.name = name;
		}

		public String toString() {
			return String.format("[%06d] user=%5.2f%% sys=%5.2f%% - %s", id, userT, (sysT), name);
		}		
	}
	
	private static class UserTimeComparator implements Comparator<ThreadLine> {

		@Override
		public int compare(ThreadLine o1, ThreadLine o2) {
			return Double.compare(o2.userT, o1.userT);
		}
	}

	private static class SysTimeComparator implements Comparator<ThreadLine> {
		
		@Override
		public int compare(ThreadLine o1, ThreadLine o2) {
			return Double.compare(o2.sysT, o1.sysT);
		}
	}

	private static class CpuTimeComparator implements Comparator<ThreadLine> {
		
		@Override
		public int compare(ThreadLine o1, ThreadLine o2) {
			return Double.compare(o2.userT + o2.sysT, o1.userT + o1.sysT);
		}
	}

	private static class ThreadNameComparator implements Comparator<ThreadLine> {
		
		@Override
		public int compare(ThreadLine o1, ThreadLine o2) {
			return o1.name.compareTo(o2.name);
		}
	}

	private static class AllocRateComparator implements Comparator<ThreadLine> {
		
		@Override
		public int compare(ThreadLine o1, ThreadLine o2) {
			return Double.compare(o2.allocRate, o1.allocRate);
		}
	}
}
