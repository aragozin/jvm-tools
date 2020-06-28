package org.gridkit.jvmtool;

public interface SafePointMonitor {

    public long getSafePointCount();

    public long getSafePointSyncTime();

    public long getSafePointTime();

}
