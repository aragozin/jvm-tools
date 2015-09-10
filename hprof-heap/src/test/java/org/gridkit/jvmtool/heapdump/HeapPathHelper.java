package org.gridkit.jvmtool.heapdump;

import java.util.Set;

import org.gridkit.jvmtool.heapdump.PathStep.Move;
import org.netbeans.lib.profiler.heap.Instance;

public class HeapPathHelper {

    public static String trackFirst(Instance i, String path) {
        Set<Move> moves = HeapPath.track(i, HeapPath.parsePath(path, true));
        for(Move m: moves) {
            return m.pathSpec;
        }
        return null;
    }    
}
