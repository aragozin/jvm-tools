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
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.gridkit.util.formating.Formats;

public class MBeanCpuUsageReporter {

	private MBeanServerConnection mserver;

	private long lastTimestamp;
	private long lastProcessCpuTime;
	private BigInteger lastCummulativeCpuTime;
	private BigInteger lastCummulativeUserTime;
	
	private Map<Long, CompositeData> threadDump = new HashMap<Long, CompositeData>();
	private Map<Long, ThreadNote> notes = new HashMap<Long, MBeanCpuUsageReporter.ThreadNote>();
	
	private List<Comparator<ThreadLine>> comparators = new ArrayList<Comparator<ThreadLine>>();
	
	private int topLimit = Integer.MAX_VALUE;
	
	private Pattern filter;
	
	public MBeanCpuUsageReporter(MBeanServerConnection mserver) {
		this.mserver = mserver;
		
		lastTimestamp = System.nanoTime();
		lastProcessCpuTime = getProcessCpuTime();
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
		
		Map<Long,ThreadNote> newNotes = new HashMap<Long, ThreadNote>();
		
		BigInteger totalCpu = BigInteger.valueOf(0);
		BigInteger totalUser = BigInteger.valueOf(0);
		
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
			
			newNotes.put(tid, newNote);
			
			totalCpu = totalCpu.add(BigInteger.valueOf(newNote.lastCpuTime));
			totalUser = totalUser.add(BigInteger.valueOf(newNote.lastUserTime));
			
			if (lastNote != null) {

				double cpuT = ((double)(newNote.lastCpuTime - lastNote.lastCpuTime)) / timeSplit;
				double userT = ((double)(newNote.lastUserTime - lastNote.lastUserTime)) / timeSplit;

				table.add(new ThreadLine(tid, 100 * userT, 100 * (cpuT - userT), getThreadName(tid)));
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

			sb.append(Formats.toDatestamp(System.currentTimeMillis()));
			sb.append(String.format(" CPU usage \n  process cpu=%.2f%%\n  application: cpu=%.2f%% (user=%.2f%% sys=%.2f%%)\n  other: cpu=%.2f%% \n", 100 * processT, 100 * cpuT, 100 * userT, 100 * (cpuT - userT), 100 * (processT - cpuT)));
			for(ThreadLine line: table) {
				sb.append(line).append('\n');
			}
			sb.append("\n");			
		}
		
		lastTimestamp = currentTime;
		notes = newNotes;
		lastCummulativeCpuTime = totalCpu;
		lastCummulativeUserTime = totalUser;
		lastProcessCpuTime = currentCpuTime;
		
		return sb.toString();
	}

	private void dumpThreads() {
		try {
			ObjectName bean = new ObjectName("java.lang:type=Threading");
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
			ObjectName bean = new ObjectName("java.lang:type=Threading");
			long time = (Long) mserver.invoke(bean, "getThreadCpuTime", new Object[]{tid}, new String[]{"long"});
			return time;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private long getThreadUserTime(long tid) {
		try {
			ObjectName bean = new ObjectName("java.lang:type=Threading");
			long time = (Long) mserver.invoke(bean, "getThreadUserTime", new Object[]{tid}, new String[]{"long"});
			return time;
		} catch (Exception e) {
			throw new RuntimeException(e);
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
		
	}
	
	private static class ThreadLine {
		
		long id;
		double userT;
		double sysT;
		String name;
		
		public ThreadLine(long id, double userT, double sysT, String name) {
			this.id = id;
			this.userT = userT;
			this.sysT = sysT;
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
			return Double.compare(o2.userT, o1.userT);
		}
	}

	private static class CpuTimeComparator implements Comparator<ThreadLine> {
		
		@Override
		public int compare(ThreadLine o1, ThreadLine o2) {
			return Double.compare(o2.userT + o2.sysT, o1.userT + o1.userT);
		}
	}

	private static class ThreadNameComparator implements Comparator<ThreadLine> {
		
		@Override
		public int compare(ThreadLine o1, ThreadLine o2) {
			return o1.name.compareTo(o2.name);
		}
	}
}
