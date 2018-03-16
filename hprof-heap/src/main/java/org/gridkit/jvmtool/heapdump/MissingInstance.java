package org.gridkit.jvmtool.heapdump;

import java.util.Collections;
import java.util.List;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.Value;

public class MissingInstance implements Instance {

	private final long instanceId;

	public MissingInstance(long instanceId) {
		this.instanceId = instanceId;
	}

	@Override
	public List<FieldValue> getFieldValues() {
		return Collections.emptyList();
	}

	@Override
	public boolean isGCRoot() {
		return false;
	}

	@Override
	public long getInstanceId() {
		return instanceId;
	}

	@Override
	public int getInstanceNumber() {
		return 0;
	}

	@Override
	public JavaClass getJavaClass() {
		return MissingInstanceType.INSTANCE;
	}

	@Override
	public Instance getNearestGCRootPointer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getReachableSize() {
		return 0;
	}

	@Override
	public List<Value> getReferences() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getRetainedSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getSize() {
		return 0;
	}

	@Override
	public List<FieldValue> getStaticFieldValues() {
		return Collections.emptyList();
	}

	@Override
	public Object getValueOfField(String name) {
		return null;
	}
}
