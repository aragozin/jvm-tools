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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread stack sampler.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ThreadDumpSampler {

//	private static final ObjectName THREADING_MBEAN = name("java.lang:type=Threading");
//	private static ObjectName name(String name) {
//		try {
//			return new ObjectName(name);
//		} catch (MalformedObjectNameException e) {
//			throw new RuntimeException(e);
//		}
//	}

	boolean collectCpu = true;
	boolean collectUserCpu = true;
	boolean collectAllocation = true;
	private ThreadMXBean threading;

	private Pattern threadFilter;
	private long[] threadSet;
	private List<CounterCollector> collectors = new ArrayList<CounterCollector>();

	public ThreadDumpSampler() {
	}

	public void setThreadFilter(String pattern) {
	    this.threadFilter = Pattern.compile(pattern);
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
	    this.threading = threadingMBean;
	    collectors.clear();
	    if (collectCpu) {
	        collectors.add(new GenericMBeanThreadCounter(threadingMBean, CounterType.CPU_TIME));
	    }
	    if (collectUserCpu) {
	        collectors.add(new GenericMBeanThreadCounter(threadingMBean, CounterType.USER_TIME));
	    }
	    if (collectAllocation) {
	        collectors.add(new GenericMBeanThreadCounter(threadingMBean, CounterType.ALLOCATED_BYTES));
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
            if (threadFilter == null || threadFilter.matcher(name).matches()) {
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
	        dump = compactThreads(threading.getThreadInfo(threadSet, Integer.MAX_VALUE));
	    }
	    else {
	        dump = filterThreads(threading.dumpAllThreads(false, false));

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
	    ThreadSnapshot ts = new ThreadSnapshot();
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
	        Matcher m = threadFilter.matcher("");
            int n = 0;
            for(int i = 0; i != dumpAllThreads.length; ++i) {
                if (dumpAllThreads[i] != null) {
                    m.reset(dumpAllThreads[i].getThreadName());
                    if (m.matches()) {
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

        public void copyToSnapshot(ThreadSnapshot snap) {
            snap.reset();
            snap.threadId = threadId;
            snap.timestamp = timestamp;
            snap.elements = getTrace();
        }
	}

    static interface CounterCollector {

        public void collect(long[] threadID);

        public void fillIntoSnapshot(ThreadSnapshot snap);

    }

    private static enum CounterType {
        CPU_TIME,
        USER_TIME,
        ALLOCATED_BYTES
    }

    private static class GenericMBeanThreadCounter implements CounterCollector {

        private ThreadMXBean slowBean;
        @SuppressWarnings("restriction")
        private com.sun.management.ThreadMXBean fastBean;
        private CounterType counter;

        private long[] threads;
        private long[] counters;
        private int n = 0;

        @SuppressWarnings("restriction")
        public GenericMBeanThreadCounter(ThreadMXBean bean, CounterType counter) {
            this.slowBean = bean;
            if (bean instanceof com.sun.management.ThreadMXBean) {
                fastBean = (com.sun.management.ThreadMXBean) bean;
            }
            this.counter = counter;
        }

        @Override
        public void collect(long[] threadID) {
            n = 0;
            threads = threadID;
            if (fastBean != null) {
                counters = callFast(threads);
            }
            else {
                for(int i = 0; i != threads.length; ++i) {
                    counters[i] = callSlow(threads[i]);
                }
            }
        }

        @SuppressWarnings("restriction")
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
        public void fillIntoSnapshot(ThreadSnapshot snap) {
            int n = indexOf(snap.threadId);
            ThreadCounter counterKey;
            switch(counter) {
                case CPU_TIME: counterKey = ThreadCounter.CPU_TIME; break;
                case USER_TIME: counterKey = ThreadCounter.USER_TIME; break;
                case ALLOCATED_BYTES: counterKey = ThreadCounter.ALLOCATED_BYTES; break;
                default: throw new RuntimeException("Unknown counter: " + counter);
            }
            snap.setCounter(counterKey, counters[n]);
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
