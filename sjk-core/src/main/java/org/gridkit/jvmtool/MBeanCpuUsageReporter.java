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

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.gridkit.jvmtool.stacktrace.ThreadMXBeanEx;
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
	private ThreadMXBean mbean;

	private long lastTimestamp;
	private long lastProcessCpuTime;
	private long lastYougGcCpuTime;
	private long lastOldGcCpuTime;
	private long lastSafePointCount;
	private long lastSafePointTime;
	private long lastSafePointSyncTime;
	private BigInteger lastCummulativeCpuTime = BigInteger.valueOf(0);
	private BigInteger lastCummulativeUserTime  = BigInteger.valueOf(0);
	private BigInteger lastCummulativeAllocatedAmount = BigInteger.valueOf(0);
	
	private Map<Long, ThreadTrac> threadDump = new HashMap<Long, ThreadTrac>();
	private Map<Long, ThreadNote> notes = new HashMap<Long, ThreadNote>();
	
	private List<Comparator<ThreadLine>> comparators = new ArrayList<Comparator<ThreadLine>>();
	
	private int topLimit = Integer.MAX_VALUE;
	
	private Pattern filter;
	
	private boolean bulkCpuEnabled;
	private boolean threadAllocatedMemoryEnabled;
	
	private GcCpuUsageMonitor gcMon;
	private SafePointMonitor spMon;
	
	public MBeanCpuUsageReporter(MBeanServerConnection mserver) {
		this.mserver = mserver;
		this.mbean = ThreadMXBeanEx.BeanHelper.connectThreadMXBean(mserver);
		
		threadAllocatedMemoryEnabled = getThreadingMBeanCapability("ThreadAllocatedMemoryEnabled");
		bulkCpuEnabled = verifyBulkCpu();
		
		lastTimestamp = System.nanoTime();
		lastProcessCpuTime = getProcessCpuTime();
	}
	
	public void setGcCpuUsageMonitor(GcCpuUsageMonitor gcMon) {
	    this.gcMon = gcMon;
	}

	public void setSafePointMonitor(SafePointMonitor spMon) {
	    this.spMon = spMon;
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
	
	private boolean verifyBulkCpu() {
	    try {
            long[] ids = mbean.getAllThreadIds();
            ((ThreadMXBeanEx)mbean).getThreadCpuTime(ids);
            ((ThreadMXBeanEx)mbean).getThreadUserTime(ids);
            return true;
        } catch (Exception e) {
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
	
	public void probe() {
    	try {
    	    long[] ids = mbean.getAllThreadIds();
    	    ThreadInfo[] ti = mbean.getThreadInfo(ids);
    
    	    Map<Long, ThreadInfo> buf = new HashMap<Long, ThreadInfo>();
    	    for(ThreadInfo t: ti) {
    	        if (t != null) {
    	            buf.put(t.getThreadId(), t);
    	        }
    	    }
    	    
    	    for(Long key: threadDump.keySet()) {
    	        ThreadTrac tt = threadDump.get(key);
    	        ThreadInfo t = buf.remove(key);
    	        if (t != null) {
        	        tt.name = t.getThreadName();
        	        tt.lastThreadInfo = t;
    	        }
    	        else {
    	            tt.dead = true;
    	        }
    	    }
    	    
    	    for(ThreadInfo t: buf.values()) {
    	        ThreadTrac tt = new ThreadTrac();
    	        tt.name = t.getThreadName();
    	        tt.lastThreadInfo = t;
    	        threadDump.put(t.getThreadId(), tt);
    	    }
    	    
    	    if (threadAllocatedMemoryEnabled) {
    	        long[] alloc = ((ThreadMXBeanEx)mbean).getThreadAllocatedBytes(ids);
    	        for (int i = 0 ; i != ids.length; ++i) {
    	            threadDump.get(ids[i]).lastAllocatedBytes = alloc[i];
    	        }
    	    }
    	    
    	    if (bulkCpuEnabled) {
    	        long[] cpu = ((ThreadMXBeanEx)mbean).getThreadCpuTime(ids);
    	        long[] usr = ((ThreadMXBeanEx)mbean).getThreadUserTime(ids);
                for (int i = 0 ; i != ids.length; ++i) {
                    threadDump.get(ids[i]).lastCpuTime = cpu[i];
                    threadDump.get(ids[i]).lastUserTime = usr[i];
                }
    	    }
    	    else {
    	        for(long id: ids) {
    	            ThreadTrac tt = threadDump.get(id);
    	            tt.lastCpuTime = mbean.getThreadCpuTime(id);
    	            tt.lastUserTime = mbean.getThreadCpuTime(id);
    	        }
    	    }
    	    
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

	private void cleanDead() {
	    Iterator<ThreadTrac> it = threadDump.values().iterator();
	    while(it.hasNext()) {
	        ThreadTrac tt = it.next();
	        if (tt.dead) {
	            it.remove();
	        }
	    }
	}
	
    public String report() {

		probe();
		
		StringBuilder sb = new StringBuilder();
		
		long currentTime = System.nanoTime();
		long timeSplit = currentTime - lastTimestamp;
		long currentCpuTime = getProcessCpuTime();
		long currentYoungGcCpuTime = gcMon == null ? 0 : gcMon.getYoungGcCpu();
		long currentOldGcCpuTime = gcMon == null ? 0 : gcMon.getOldGcCpu();
		long currentSafePointCount = spMon == null ? 0 : spMon.getSafePointCount();
		long currentSafePointTime = spMon == null ? 0 : spMon.getSafePointTime();
		long currentSafePointSyncTime = spMon == null ? 0 : spMon.getSafePointSyncTime();
		
		Map<Long,ThreadNote> newNotes = new HashMap<Long, ThreadNote>();
		
		BigInteger deltaCpu = BigInteger.valueOf(0);
		BigInteger deltaUser = BigInteger.valueOf(0);
		BigInteger deltaAlloc = BigInteger.valueOf(0);
		
		List<ThreadLine> table = new ArrayList<ThreadLine>();
		
		for(long tid: getAllThreadIds()) {
			
			String threadName = getThreadName(tid);
			
			ThreadNote lastNote = notes.get(tid);
			ThreadNote newNote = new ThreadNote();
			newNote.lastCpuTime = getThreadCpuTime(tid);
			newNote.lastUserTime = getThreadUserTime(tid);
			newNote.lastAllocatedBytes = getThreadAllocatedBytes(tid);
			
			newNotes.put(tid, newNote);
			
			long lastCpu = lastNote == null ? 0 : lastNote.lastCpuTime;
			long lastUser = lastNote == null ? 0 : lastNote.lastUserTime;
			long lastAlloc = lastNote == null ? 0 : lastNote.lastAllocatedBytes;
			
			deltaCpu = deltaCpu.add(BigInteger.valueOf(newNote.lastCpuTime - lastCpu));
			deltaUser = deltaUser.add(BigInteger.valueOf(newNote.lastUserTime - lastUser));
			deltaAlloc = deltaAlloc.add(BigInteger.valueOf(newNote.lastAllocatedBytes - lastAlloc));
			
			if (lastNote != null) {

			    if (filter != null && !filter.matcher(threadName).matches()) {
			        continue;
			    }
			    
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
			double cpuT = ((double)(deltaCpu.longValue())) / timeSplit;
			double userT = ((double)(deltaUser.longValue())) / timeSplit;
			double allocRate = ((double)(deltaAlloc.longValue())) * TimeUnit.SECONDS.toNanos(1) / timeSplit;

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
			if (currentSafePointCount > 0) {
			    double spRate = (TimeUnit.SECONDS.toNanos(1) * (double)(currentSafePointCount - lastSafePointCount)) / timeSplit;
			    double spCpuUsage = ((double)(currentSafePointTime - lastSafePointTime)) / timeSplit;
			    double spSyncCpuUsage = ((double)(currentSafePointSyncTime - lastSafePointSyncTime)) / timeSplit;
			    double spAvg = ((double)(currentSafePointTime + currentSafePointSyncTime - lastSafePointTime - lastSafePointSyncTime)) / (currentSafePointCount - lastSafePointCount) / TimeUnit.MILLISECONDS.toNanos(1);
			    sb.append(String.format("  safe point rate: %.1f (events/s) avg. safe point pause: %.2fms\n", spRate, spAvg));			    
			    sb.append(String.format("  safe point sync time: %.2f%% processing time: %.2f%% (wallclock time)\n", 100 * spSyncCpuUsage, 100 * spCpuUsage));			    
			}
			for(ThreadLine line: table) {
				sb.append(format(line)).append('\n');
			}
			sb.append("\n");			
		}
		
		lastTimestamp = currentTime;
		notes = newNotes;
		lastCummulativeCpuTime = lastCummulativeCpuTime.add(deltaCpu);
		lastCummulativeUserTime = lastCummulativeUserTime.add(deltaUser);
		lastCummulativeAllocatedAmount = lastCummulativeAllocatedAmount.add(deltaAlloc);
		lastProcessCpuTime = currentCpuTime;
		lastYougGcCpuTime = currentYoungGcCpuTime;
		lastOldGcCpuTime = currentOldGcCpuTime;
		lastSafePointCount = currentSafePointCount;
		lastSafePointTime = currentSafePointTime;
		lastSafePointSyncTime = currentSafePointSyncTime;
	
		cleanDead();
		
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

	private String getThreadName(long tid) {
	    return threadDump.get(tid).name;
	}

	private Collection<Long> getAllThreadIds() {
		return new TreeSet<Long>(threadDump.keySet());
	}

	private long getThreadCpuTime(long tid) {
	    return threadDump.get(tid).lastCpuTime;
	}

	private long getThreadUserTime(long tid) {
	    return threadDump.get(tid).lastUserTime;
	}
	
	private long getThreadAllocatedBytes(long tid) {
	    return threadDump.get(tid).lastAllocatedBytes;
	}

	private long getProcessCpuTime() {
		try {
			ObjectName bean = new ObjectName("java.lang:type=OperatingSystem");
			return (Long) mserver.getAttribute(bean, "ProcessCpuTime");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class ThreadTrac {
	    
	    private String name;
	    private long lastCpuTime;
	    private long lastUserTime;
	    private long lastAllocatedBytes;
	    @SuppressWarnings("unused")
        private ThreadInfo lastThreadInfo;
	    
	    private boolean dead;
	    
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
