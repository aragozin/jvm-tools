package org.gridkit.jvmtool.heapdump;

import org.netbeans.lib.profiler.heap.Instance;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface InstanceCallback {

    public void feed(Instance instance);

}
