package org.gridkit.jvmtool.stacktrace;

public interface ThreadNameFilter {

    public boolean accept(String threadName);

}
