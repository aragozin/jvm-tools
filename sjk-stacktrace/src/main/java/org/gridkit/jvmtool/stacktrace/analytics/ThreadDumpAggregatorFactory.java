package org.gridkit.jvmtool.stacktrace.analytics;

import java.lang.Thread.State;

public interface ThreadDumpAggregatorFactory {

    public static CommonFactory COMMON = new CommonFactory();
    
    public ThreadDumpAggregator newInstance();
    
    public static class CommonFactory {
        
        public ThreadDumpAggregatorFactory maxTimestamp() {
            return new MaxTimestampAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory minTimestamp() {
            return new MinTimestampAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory count() {
            return new CountAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory cpu() {
            return new CpuAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory sysCpu() {
        	return new SysCpuAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory alloc() {
            return new AllocAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory threadState(State state) {
            return new ThreadStateAggregatorFactory(state);
        }

        public ThreadDumpAggregatorFactory threadFilter(ThreadSnapshotFilter filter) {
            return new FilterAggregatorFactory(filter);
        }

        public ThreadDumpAggregatorFactory waitCalls() {
            return new WaitCallsAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory inNative() {
            return new NativeAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory frequency() {
            return new FrequencyAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory periodCHM() {
            return new PeriodCHMAggregatorFactory();
        }

        public ThreadDumpAggregatorFactory frequencyHM() {
            return new FrequencyHMAggregatorFactory();
        }        

        public ThreadDumpAggregatorFactory name() {
            return new ThreadNameAggregatorFactory(32);
        }        

        public ThreadDumpAggregatorFactory name(int length) {
            return new ThreadNameAggregatorFactory(length);
        }        

        public ThreadDumpAggregatorFactory threadId() {
            return new ThreadIdAggregatorFactory();
        }        
    }
}
