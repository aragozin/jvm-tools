package org.gridkit.jvmtool.osdiag;

public abstract class ProcTimeHelper {

    private static ProcTimeHelper INSTANCE = initHelper();

    public ProcTimeHelper getInstance() {
        return INSTANCE;
    }

    private static ProcTimeHelper initHelper() {
        // TODO Auto-generated method stub
        return null;
    }

    public abstract boolean getProcessTime(long pid, TimeInfoMS time);

    public abstract boolean getThreadTime(long pid, TimeInfoMS time);

}
