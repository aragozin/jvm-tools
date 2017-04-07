package org.gridkit.jvmtool.gcmon;

public class SimpleMemoryPoolEventEncoder implements MemoryPoolEventConsumer {

	private final MemoryPoolEventWriter writer;

	public SimpleMemoryPoolEventEncoder(MemoryPoolEventWriter writer) {
		this.writer = writer;
	}

	@Override
	public void consumeUsageEvent(MemoryPoolInfoEvent event) {
		writer.storeMemPoolInfo(event);		
	}

	@Override
	public void consumePeakEvent(MemoryPoolInfoEvent event) {
		writer.storeMemPoolInfo(event);		
	}

	@Override
	public void consumeCollectionUsageEvent(MemoryPoolInfoEvent event) {
		writer.storeMemPoolInfo(event);		
	}
}
