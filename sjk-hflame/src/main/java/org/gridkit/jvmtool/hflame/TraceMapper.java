package org.gridkit.jvmtool.hflame;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public interface TraceMapper {

    public GenericStackElement generateTraceTerminator(ThreadSnapshot snap);

}
