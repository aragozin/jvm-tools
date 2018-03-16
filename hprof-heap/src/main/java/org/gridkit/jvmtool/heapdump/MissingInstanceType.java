package org.gridkit.jvmtool.heapdump;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

public class MissingInstanceType implements JavaClass {

	public static final MissingInstanceType INSTANCE = new MissingInstanceType();
	
	private MissingInstanceType() {
	}
	
	@Override
	public Object getValueOfStaticField(String name) {
		return null;
	}

	@Override
	public long getAllInstancesSize() {
		return 0;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public Instance getClassLoader() {
		return null;
	}

	@Override
	public List<Field> getFields() {
		return Collections.emptyList();
	}

	@Override
	public int getInstanceSize() {
		return 0;
	}

	@Override
	public List<Instance> getInstances() {
		return Collections.emptyList();
	}

	@Override
	public int getInstancesCount() {
		return 0;
	}

	@Override
	public long getRetainedSizeByClass() {
		return 0;
	}

	@Override
	public long getJavaClassId() {
		return -1;
	}

	@Override
	public String getName() {
		return "<NO SUCH INSTANCE>";
	}

	@Override
	public List<FieldValue> getStaticFieldValues() {
		return Collections.emptyList();
	}

	@Override
	public Collection<JavaClass> getSubClasses() {
		return Collections.emptySet();
	}

	@Override
	public JavaClass getSuperClass() {
		return null;
	}
}
