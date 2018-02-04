/**
 * Copyright 2014 Alexey Ragozin
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
package org.gridkit.jvmtool.stacktrace;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectName;

/**
 * Thread stack sampler.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ThreadDumpSampler {

    @SuppressWarnings("restriction")
    public static ThreadMXBean adaptThreadMXBean(ThreadMXBean bean) {
        try {
            if (bean instanceof com.sun.management.ThreadMXBean) {
                final com.sun.management.ThreadMXBean beanX = (com.sun.management.ThreadMXBean) bean;
                bean = new ThreadMXBeanEx() {
                    
                    @SuppressWarnings("unused")
                    /* Method added in Java 7, required for compilation */
                    public ObjectName getObjectName() {
                        return ThreadMXBeanEx.THREADING_MBEAN;
                    }

                    public int getPeakThreadCount() {
                        return beanX.getPeakThreadCount();
                    }

                    public int getDaemonThreadCount() {
                        return beanX.getDaemonThreadCount();
                    }

                    public long[] getAllThreadIds() {
                        return beanX.getAllThreadIds();
                    }

                    public ThreadInfo[] getThreadInfo(long[] ids) {
                        return beanX.getThreadInfo(ids);
                    }

                    public long getCurrentThreadCpuTime() {
                        return beanX.getCurrentThreadCpuTime();
                    }

                    public long getCurrentThreadUserTime() {
                        return beanX.getCurrentThreadUserTime();
                    }

                    public long getThreadUserTime(long id) {
                        return beanX.getThreadUserTime(id);
                    }

                    public long[] findMonitorDeadlockedThreads() {
                        return beanX.findMonitorDeadlockedThreads();
                    }

                    public long[] findDeadlockedThreads() {
                        return beanX.findDeadlockedThreads();
                    }

                    public ThreadInfo[] dumpAllThreads(boolean lockedMonitors, boolean lockedSynchronizers) {
                        return beanX.dumpAllThreads(lockedMonitors, lockedSynchronizers);
                    }

                    public long[] getThreadAllocatedBytes(long[] arg0) {
                        return beanX.getThreadAllocatedBytes(arg0);
                    }

                    public int getThreadCount() {
                        return beanX.getThreadCount();
                    }

                    public long getThreadCpuTime(long id) {
                        return beanX.getThreadCpuTime(id);
                    }

                    public long[] getThreadCpuTime(long[] arg0) {
                        return beanX.getThreadCpuTime(arg0);
                    }

                    public ThreadInfo getThreadInfo(long id) {
                        return beanX.getThreadInfo(id);
                    }

                    public ThreadInfo getThreadInfo(long id, int maxDepth) {
                        return beanX.getThreadInfo(id, maxDepth);
                    }

                    public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
                        return beanX.getThreadInfo(ids, maxDepth);
                    }

                    public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors, boolean lockedSynchronizers) {
                        return beanX.getThreadInfo(ids, lockedMonitors, lockedSynchronizers);
                    }

                    public long[] getThreadUserTime(long[] arg0) {
                        return beanX.getThreadUserTime(arg0);
                    }

                    public long getTotalStartedThreadCount() {
                        return beanX.getTotalStartedThreadCount();
                    }

                    public boolean isCurrentThreadCpuTimeSupported() {
                        return beanX.isCurrentThreadCpuTimeSupported();
                    }

                    public boolean isObjectMonitorUsageSupported() {
                        return beanX.isObjectMonitorUsageSupported();
                    }

                    public boolean isSynchronizerUsageSupported() {
                        return beanX.isSynchronizerUsageSupported();
                    }

                    public boolean isThreadContentionMonitoringSupported() {
                        return beanX.isThreadContentionMonitoringSupported();
                    }

                    public boolean isThreadContentionMonitoringEnabled() {
                        return beanX.isThreadContentionMonitoringEnabled();
                    }

                    public boolean isThreadCpuTimeSupported() {
                        return beanX.isThreadCpuTimeSupported();
                    }

                    public boolean isThreadCpuTimeEnabled() {
                        return beanX.isThreadCpuTimeEnabled();
                    }

                    public void resetPeakThreadCount() {
                        beanX.resetPeakThreadCount();
                    }

                    public void setThreadContentionMonitoringEnabled(boolean enable) {
                        beanX.setThreadContentionMonitoringEnabled(enable);
                    }

                    public void setThreadCpuTimeEnabled(boolean enable) {
                        beanX.setThreadCpuTimeEnabled(enable);
                    }
                };
            }
        }
        catch(NoClassDefFoundError e) {
            // ignore
        }
        catch(Exception e) {
            // ignore
        }
        return bean;
    }

    boolean collectTraces = true;
	boolean collectCpu = true;
	boolean collectUserCpu = true;
	boolean collectAllocation = true;
	private ThreadMXBean threading;

	private ThreadNameFilter threadFilter;
	private long[] threadSet;
	private List<CounterCollector> collectors = new ArrayList<CounterCollector>();

	public ThreadDumpSampler() {
	}

	public void setThreadFilter(final String pattern) {
	    this.threadFilter = new ThreadNameFilter() {
            
	        final Matcher matcher = Pattern.compile(pattern).matcher(""); 
	        
            @Override
            public boolean accept(String threadName) {
            	if (threadName != null) {
	                matcher.reset(threadName);
	                return matcher.matches();
            	}
            	else {
            		return false;
            	}
            }
        };
	}

	public void setThreadFilter(final ThreadNameFilter filter) {
	    this.threadFilter = filter;
	}
	
	public void enableThreadStackTrace(boolean enable) {
	    collectTraces = enable;
	}
	
	public void enableThreadCpu(boolean enable) {
	    collectCpu = true;
	}

	public void enableThreadUserCpu(boolean enable) {
	    collectUserCpu = true;
	}

	public void enableThreadAllocation(boolean enable) {
	    collectAllocation = true;
	}

	public void connect(ThreadMXBean threadingMBean) {
	    this.threading = adaptThreadMXBean(threadingMBean);
	    collectors.clear();
	    if (collectCpu) {
	        collectors.add(new GenericMBeanThreadCounter(threading, CounterType.CPU_TIME));
	    }
	    if (collectUserCpu) {
	        collectors.add(new GenericMBeanThreadCounter(threading, CounterType.USER_TIME));
	    }
	    if (collectAllocation) {
	        collectors.add(new GenericMBeanThreadCounter(threading, CounterType.ALLOCATED_BYTES));
	    }
	}

	/**
	 * Find threads according to thread name filters and memorise their IDs.
	 * <br/>
	 * Optional method to avoid dump all threads at every collection.
	 */
	public void prime() {
        ThreadInfo[] ti = threading.dumpAllThreads(false, false);
        long[] tids = new long[ti.length];
        int n = 0;
        for(ThreadInfo t:ti) {
            long tid = t.getThreadId();
            String name = t.getThreadName();
            if (threadFilter == null || threadFilter.accept(name)) {
                tids[n++] = tid;
            }
        }
        tids = Arrays.copyOf(tids, n);
        threadSet = tids;
	}

	public void collect(StackTraceWriter writer) throws IOException {
	    long timestamp = System.currentTimeMillis();
	    ThreadInfo[] dump;
	    if (threadSet != null) {
	        if (collectTraces) {
	            dump = compactThreads(threading.getThreadInfo(threadSet, Integer.MAX_VALUE));
	        }
	        else {
	            dump = compactThreads(threading.getThreadInfo(threadSet));
	        }
	    }
	    else {
	        if (collectTraces) {
	            dump = filterThreads(threading.dumpAllThreads(false, false));
	        }
	        else {
	            dump = filterThreads(threading.getThreadInfo(threading.getAllThreadIds()));
	        }

	    }
	    long[] ids = new long[dump.length];
	    for(int i = 0; i != dump.length; ++i) {
	        ids[i] = dump[i].getThreadId();
	    }
	    for(CounterCollector cc: collectors) {
	        try {
	            cc.collect(ids);
	        }
	        catch(Exception e) {
	            // ignore
	        }
	    }
	    ThreadCapture ts = new ThreadCapture();
	    for(ThreadInfo ti: dump) {
	        ts.reset();
	        ts.timestamp = timestamp;
	        ts.copyFrom(ti);
	        for (CounterCollector cc: collectors) {
	            try {
	                cc.fillIntoSnapshot(ts);
	            }
	            catch(Exception e) {
	                // ignore
	            }
	        }
	        writer.write(ts);
	    }
	}

	private ThreadInfo[] compactThreads(ThreadInfo[] dumpAllThreads) {
	    int n = 0;
        for(int i = 0; i != dumpAllThreads.length; ++i) {
            if (dumpAllThreads[i] != null) {
                ++n;
            }
        }
        if (n == dumpAllThreads.length) {
            return dumpAllThreads;
        }
        else {
            ThreadInfo[] result = new ThreadInfo[n];
            n = 0;
            for(ThreadInfo ti: dumpAllThreads) {
                if (ti != null) {
                    result[n++] = ti;
                }
            }
            return result;
        }
	}

	private ThreadInfo[] filterThreads(ThreadInfo[] dumpAllThreads) {
	    if (threadFilter == null) {
	        return compactThreads(dumpAllThreads);
	    }
	    else {
            int n = 0;
            for(int i = 0; i != dumpAllThreads.length; ++i) {
                if (dumpAllThreads[i] != null) {
                    if (threadFilter.accept(dumpAllThreads[i].getThreadName())) {
                        ++n;
                    }
                    else {
                        dumpAllThreads[i] = null;
                    }
                }
            }
            if (n == dumpAllThreads.length) {
                return dumpAllThreads;
            }
            else {
                ThreadInfo[] result = new ThreadInfo[n];
                n = 0;
                for(ThreadInfo ti: dumpAllThreads) {
                    if (ti != null) {
                        result[n++] = ti;
                    }
                }
                return result;
            }
	    }
    }

    @SuppressWarnings("serial")
    public static class Trace implements Serializable {

	    private long threadId;
	    private long timestamp;
	    private int[] trace;

	    private StackTraceElement[] traceDictionary;

        public Trace(long threadId, long timestamp, int[] trace) {
            this.threadId = threadId;
            this.timestamp = timestamp;
            this.trace = trace;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getThreadId() {
            return threadId;
        }

        public StackTraceElement[] getTrace() {
            StackTraceElement[] strace = new StackTraceElement[trace.length];
            for(int i = 0; i != strace.length; ++i) {
                strace[i] = traceDictionary[trace[i]];
            }
            return strace;
        }

        public void copyToSnapshot(ThreadCapture snap) {
            snap.reset();
            snap.threadId = threadId;
            snap.timestamp = timestamp;
            snap.elements = getTrace();
        }
	}

    static interface CounterCollector {

        public void collect(long[] threadID);

        public void fillIntoSnapshot(ThreadCapture snap);

    }

    private static enum CounterType {
        CPU_TIME,
        USER_TIME,
        ALLOCATED_BYTES
    }

    private static class GenericMBeanThreadCounter implements CounterCollector {

        private ThreadMXBean slowBean;
        private ThreadMXBeanEx fastBean;
        private CounterType counter;

        private long[] threads;
        private long[] counters;
        private int n = 0;

        public GenericMBeanThreadCounter(ThreadMXBean bean, CounterType counter) {
            this.slowBean = bean;
            try {
                if (bean instanceof ThreadMXBeanEx) {
                    fastBean = (ThreadMXBeanEx) bean;
                }
            }
            catch(NoClassDefFoundError e) {
                // ignore
            }
            catch(Exception e) {
                // ignore
            }
            this.counter = counter;
        }

        @Override
        public void collect(long[] threadID) {
            n = 0;
            threads = threadID;
            if (fastBean != null) {
                try {
                    counters = callFast(threads);
                }
                catch(Exception e) {
                    // fallback to slow mode
                    fastBean = null;
                    collect(threadID);
                }
            }
            else {
                counters = new long[threads.length];
                for(int i = 0; i != threads.length; ++i) {
                    counters[i] = callSlow(threads[i]);
                }
            }
        }

        private long[] callFast(long[] threads) {
            switch(counter) {
                case CPU_TIME: return fastBean.getThreadCpuTime(threads);
                case USER_TIME: return fastBean.getThreadUserTime(threads);
                case ALLOCATED_BYTES: return fastBean.getThreadAllocatedBytes(threads);
                default: throw new RuntimeException("Unknown counter: " + counter);
            }
        }

        private long callSlow(long threadId) {
            switch (counter) {
                case CPU_TIME: return slowBean.getThreadCpuTime(threadId);
                case USER_TIME: return slowBean.getThreadUserTime(threadId);
                case ALLOCATED_BYTES: return -1;
                default: throw new RuntimeException("Unknown counter: " + counter);
            }
        }

        @Override
        public void fillIntoSnapshot(ThreadCapture snap) {
            int n = indexOf(snap.threadId);
            String counterKey;            
            switch(counter) {
                case CPU_TIME: counterKey = ThreadCounters.CPU_TIME_MS; break;
                case USER_TIME: counterKey = ThreadCounters.USER_TIME_MS; break;
                case ALLOCATED_BYTES: counterKey = ThreadCounters.ALLOCATED_BYTES; break;
                default: throw new RuntimeException("Unknown counter: " + counter);
            }
            long v = counters[n];
            if (v >= 0 && counter != CounterType.ALLOCATED_BYTES) {
                v = TimeUnit.NANOSECONDS.toMillis(v);
            }
            snap.counters.set(counterKey, v);
        }

        private int indexOf(long threadId) {
            if (threads[n] == threadId) {
                return n;
            }
            n++;
            if (threads[n] == threadId) {
                return n;
            }
            for(int i = 0; i != threads.length; ++i) {
                if (threads[i] == threadId) {
                    return n = i;
                }
            }
            return -1;
        }
    }
}
