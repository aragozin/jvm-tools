package org.gridkit.jvmtool.gcmon;

import org.gridkit.jvmtool.event.TypedEventWriter;

public interface MemoryPoolEventWriter extends TypedEventWriter {

	public void storeMemPoolInfo(MemoryPoolInfoEvent event);
}
