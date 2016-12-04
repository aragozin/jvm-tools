package org.gridkit.jvmtool.heapdump.io;

public interface PagePoolFactory {

    public PagePool createPagePool(int pageSize);
    
}
