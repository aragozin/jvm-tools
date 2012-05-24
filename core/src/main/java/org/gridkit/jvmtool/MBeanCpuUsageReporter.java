package org.gridkit.jvmtool;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
	
	public MBeanCpuUsageReporter(MBeanServerConnection mserver) {
		this.mserver = mserver;
		
		lastTimestamp = System.nanoTime();
		lastProcessCpuTime = getProcessCpuTime();
	}
	
	public String report() {

		dumpThreads();
		
		StringBuilder sb = new StringBuilder();
		
		long currentTime = System.nanoTime();
		long timeSplit = currentTime - lastTimestamp;
		long currentCpuTime = getProcessCpuTime();
		
		Map<Long,ThreadNote> newNotes = new HashMap<Long, ThreadNote>();
		Set<String> report = new TreeSet<String>();
		
		BigInteger totalCpu = BigInteger.valueOf(0);
		BigInteger totalUser = BigInteger.valueOf(0);
		
		
		for(long tid: getAllThreadIds()) {
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

				StringBuffer buf = new StringBuffer();
				buf.append(String.format("[%06d] user=%.2f%% sys=%.2f%% - %s", tid, 100 * userT, 100 * (cpuT - userT), getThreadName(tid)));
				report.add(buf.toString());
			}
		}
		
		if (report.size() >0) {				

			double processT = ((double)(currentCpuTime - lastProcessCpuTime)) / timeSplit;
			double cpuT = ((double)(totalCpu.subtract(lastCummulativeCpuTime).longValue())) / timeSplit;
			double userT = ((double)(totalUser.subtract(lastCummulativeUserTime).longValue())) / timeSplit;

			sb.append(Formats.toDatestamp(System.currentTimeMillis()));
			sb.append(String.format(" CPU usage \n  process cpu=%.2f%%\n  application: cpu=%.2f%% (user=%.2f%% sys=%.2f%%)\n  other: cpu=%.2f%% \n", 100 * processT, 100 * cpuT, 100 * userT, 100 * (cpuT - userT), 100 * (processT - cpuT)));
			for(String line: report) {
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

	private Object getThreadName(long tid) {
		try {
			CompositeData info = threadDump.get(tid);
			return info.get("threadName");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Collection<Long> getAllThreadIds() {
		return threadDump.keySet();
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
}
