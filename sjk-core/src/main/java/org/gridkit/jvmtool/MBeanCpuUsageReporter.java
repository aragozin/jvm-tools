/**
 * Copyright 2013-2018 Alexey Ragozin
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

import static org.gridkit.util.formating.Formats.formatRate;

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

import javax.management.Attribute;
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
	private long lastProcessOSCpuTime;
	private long lastProcessOSSysTime;
	private long lastYougGcTime;
	private long lastOldGcTime;
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
	private boolean contentionMonitoringEnabled = false;
	
	private GcCpuUsageMonitor gcMon;
	private SafePointMonitor spMon;
	private NativeThreadMonitor ntMon;
	
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

	public void setNativeThreadMonitor(NativeThreadMonitor ntMon) {
	    this.ntMon = ntMon;
	}
	
	public void setContentionMonitoringEnabled(boolean enabled) {
		if (enabled) {
			try {
				Attribute attr = new Attribute("ThreadContentionMonitoringEnabled", Boolean.TRUE);
				mserver.setAttribute(THREADING_MBEAN, attr);
			} catch (Exception e) {
				// ignore
			}			
		}
		contentionMonitoringEnabled = enabled;
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
    	            if (threadDump.get(ids[i]) == null) {
    	                continue;
    	            }
	                threadDump.get(ids[i]).lastAllocatedBytes = alloc[i];
    	        }
    	    }
    	    
    	    if (bulkCpuEnabled) {
    	        long[] cpu = ((ThreadMXBeanEx)mbean).getThreadCpuTime(ids);
    	        long[] usr = ((ThreadMXBeanEx)mbean).getThreadUserTime(ids);
                for (int i = 0 ; i != ids.length; ++i) {
                    if (threadDump.get(ids[i]) == null) {
                        continue;
                    }
                    threadDump.get(ids[i]).lastCpuTime = cpu[i];
                    threadDump.get(ids[i]).lastUserTime = usr[i];
                }
    	    }
    	    else {
    	        for(long id: ids) {
                    if (threadDump.get(id) == null) {
                        continue;
                    }
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
		long currentOsSysTime = ntMon == null ? 0 : ntMon.getProcessSysCPU();
		long currentOsCpuTime = ntMon == null ? 0 : ntMon.getProcessCPU();
		long currentYoungGcTime = gcMon == null ? 0 : gcMon.getYoungGcCpu();
		long currentOldGcTime = gcMon == null ? 0 : gcMon.getOldGcCpu();
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
			newNote.lastWaitCount = getThreadWaitCount(tid);
			newNote.lastWaitTime = getThreadWaitTime(tid);
			newNote.lastBlockCount = getThreadBlockCount(tid);
			newNote.lastBlockTime = getThreadBlockTime(tid);
			
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

				double waitRate =  ((double)(newNote.lastWaitCount - lastNote.lastWaitCount)) * TimeUnit.SECONDS.toNanos(1) / timeSplit;
				double waitT =     newNote.lastWaitTime < 0 ? Double.NaN 
						          : ((double)(newNote.lastWaitTime - lastNote.lastWaitTime)) / timeSplit;
				double blockRate = ((double)(newNote.lastBlockCount - lastNote.lastBlockCount)) * TimeUnit.SECONDS.toNanos(1) / timeSplit;
				double blockT =    newNote.lastBlockTime < 0 ? Double.NaN
						          : ((double)(newNote.lastBlockTime - lastNote.lastBlockTime)) / timeSplit;

				ThreadLine line = new ThreadLine(tid, getThreadName(tid));
				line.userT = 100 * userT;
				line.sysT = 100 * (cpuT - userT);
				line.allocRate = allocRate;
				line.waitRate = waitRate;
				line.waitT = 100 * waitT;
				line.blockRate = blockRate;
				line.blockT = 100 * blockT;
				
				table.add(line);
			}
		}
		
		int threadCount = table.size();
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

			double youngGcT = ((double)currentYoungGcTime - lastYougGcTime) / timeSplit;
			double oldGcT = ((double)currentOldGcTime - lastOldGcTime) / timeSplit;

			String osproccpu = "";
			if (currentOsCpuTime > 0) {
			    double processCpuT = ((double)(currentOsCpuTime - lastProcessOSCpuTime)) / TimeUnit.NANOSECONDS.toMicros(timeSplit);
			    double processSysT = ((double)(currentOsSysTime - lastProcessOSSysTime)) / TimeUnit.NANOSECONDS.toMicros(timeSplit);
			    osproccpu = String.format(" (OS usr+sys: %.2f%% sys: %.2f%%)", processCpuT, processSysT);
			}
			
			sb.append(Formats.toDatestamp(System.currentTimeMillis()));
			sb.append(String.format(" Process summary \n  process cpu=%.2f%%%s\n  application cpu=%.2f%% (user=%.2f%% sys=%.2f%%)\n  other: cpu=%.2f%% \n", 100 * processT, osproccpu, 100 * cpuT, 100 * userT, 100 * (cpuT - userT), 100 * (processT - cpuT), threadCount));
			int osthreadcount = ntMon == null ? 0 : ntMon.getThreadsForProcess().length;

			if (osthreadcount > threadCount) {
			    sb.append(String.format("  thread count: %d (OS threads: %d)\n", threadCount, osthreadcount));
			}
			else {
			    sb.append(String.format("  thread count: %d\n", threadCount));
			}
			if (currentYoungGcTime > 0) {
			    sb.append(String.format("  GC time=%.2f%% (young=%.2f%%, old=%.2f%%)\n", 100 * (youngGcT + oldGcT), 100 * youngGcT, 100 * oldGcT));
			}			
			if (threadAllocatedMemoryEnabled) {
				sb.append(String.format("  heap allocation rate %sb/s\n", Formats.toMemorySize((long) allocRate)));
			}
			if (currentSafePointCount > 0) {
			    if (currentSafePointCount == lastSafePointCount) {
			        sb.append(String.format("  no safe points\n"));			    
			    }
			    else {
    			    double spRate = (TimeUnit.SECONDS.toNanos(1) * (double)(currentSafePointCount - lastSafePointCount)) / timeSplit;
    			    double spCpuUsage = ((double)(currentSafePointTime - lastSafePointTime)) / timeSplit;
    			    double spSyncCpuUsage = ((double)(currentSafePointSyncTime - lastSafePointSyncTime)) / timeSplit;
    			    double spAvg = ((double)(currentSafePointTime + currentSafePointSyncTime - lastSafePointTime - lastSafePointSyncTime)) / (currentSafePointCount - lastSafePointCount) / TimeUnit.MILLISECONDS.toNanos(1);
    			    sb.append(String.format("  safe point rate: %.1f (events/s) avg. safe point pause: %.2fms\n", spRate, spAvg));			    
    			    sb.append(String.format("  safe point sync time: %.2f%% processing time: %.2f%% (wallclock time)\n", 100 * spSyncCpuUsage, 100 * spCpuUsage));
			    }
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
		lastProcessOSCpuTime = currentOsCpuTime;
		lastProcessOSSysTime = currentOsSysTime;
		lastYougGcTime = currentYoungGcTime;
		lastOldGcTime = currentOldGcTime;
		lastSafePointCount = currentSafePointCount;
		lastSafePointTime = currentSafePointTime;
		lastSafePointSyncTime = currentSafePointSyncTime;
	
		cleanDead();
		
		return sb.toString();
	}

	private Object format(ThreadLine line) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("[%06d] user=%5.2f%% sys=%5.2f%% ", line.id, line.userT, line.sysT));
		if (contentionMonitoringEnabled) {
			if (Double.isNaN(line.waitT)) {
				sb.append(String.format("wait=%s/s ", formatRate(line.waitRate)));
			}
			else {
				sb.append(String.format("wait=%s/s(%5.2f%%) ", formatRate(line.waitRate), line.waitT));
			}
			if (Double.isNaN(line.blockT)) {
				sb.append(String.format("block=%s/s ", formatRate(line.blockRate)));
			}
			else {
				sb.append(String.format("block=%s/s(%5.2f%%) ", formatRate(line.blockRate), line.blockT));
			}
		}
		if (threadAllocatedMemoryEnabled) {
			sb.append(String.format("alloc=%6sb/s ", Formats.toMemorySize((long)line.allocRate)));
		}
		sb.append(String.format("- %s", line.name));
		return sb.toString();
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

	private long getThreadWaitCount(long tid) {
		return threadDump.get(tid).lastThreadInfo.getWaitedCount();
	}

	private long getThreadWaitTime(long tid) {
		return threadDump.get(tid).lastThreadInfo.getWaitedTime() * 1000000;
	}

	private long getThreadBlockCount(long tid) {
		return threadDump.get(tid).lastThreadInfo.getBlockedCount();
	}

	private long getThreadBlockTime(long tid) {
		return threadDump.get(tid).lastThreadInfo.getBlockedTime() * 1000000;
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
		private long lastWaitCount;
		private long lastWaitTime;
		private long lastBlockCount;
		private long lastBlockTime;
	}
	
	private static class ThreadLine {
		
		long id;
		String name;
		double userT;
		double sysT;
		double allocRate;
		double waitRate;
		double waitT;
		double blockRate;
		double blockT;
		
		public ThreadLine(long id, String name) {
			this.id = id;
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
