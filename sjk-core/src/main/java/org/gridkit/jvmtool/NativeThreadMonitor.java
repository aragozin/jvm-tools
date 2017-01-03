package org.gridkit.jvmtool;

public interface NativeThreadMonitor {

    public long[] getThreadsForProcess();

    public long getProcessCPU();

    public long getProcessSysCPU();

    public long getThreadCPU(long tid);

    public long getThreadSysCPU(long tid);

}
