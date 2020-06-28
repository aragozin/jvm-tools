package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public interface PositionalStackMatcher {

    /**
     * Find frame in trace matching a certain criteria.
     * Return index of matched frame (which will be greater of equal <code>matchFrom</code>).
     *
     * @return frame index or -1 if not found
     */
    public int matchNext(ThreadSnapshot snap, int matchFrom);

}
