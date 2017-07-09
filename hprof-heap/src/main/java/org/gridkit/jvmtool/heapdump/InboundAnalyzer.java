package org.gridkit.jvmtool.heapdump;

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

public class InboundAnalyzer {

	private Heap heap;
	
	private RefSet gcRoots = new RefSet();
	private RefSet marked = new RefSet();
	private RefSet target = new RefSet();
	private RefSet inbound = new RefSet();
	
	public InboundAnalyzer(Heap heap) {
		this.heap = heap;
	}
	
	public void mark(long instanceId) {
		marked.set(instanceId, true);
		target.set(instanceId, true);
	}
	
	public void initRoots() {
		for(JavaClass jc: heap.getAllClasses()) {
			for(FieldValue fv: jc.getStaticFieldValues()) {
				if (fv instanceof ObjectFieldValue) {
					gcRoots.set(((ObjectFieldValue) fv).getInstanceId(), true);
				}
			}
		}
		for(GCRoot gcr: heap.getGCRoots()) {
			gcRoots.set(gcr.getInstance().getInstanceId(), true);
		}
	}
	
	public boolean isExhausted() {
		return target.countOnes() == 0;
	}
	
	public void report() {
		for(Instance i: heap.getAllInstances()) {
			if (marked.get(i.getInstanceId())) {
				continue;
			}
			
			if (i instanceof ObjectArrayInstance) {
				ObjectArrayInstance oai = (ObjectArrayInstance) i;
				for(Instance ii: oai.getValues()) {
					if (ii != null && ii.getInstanceId() != 0) {
						if (target.get(ii.getInstanceId())) {
							report(i, "[]",  ii.getInstanceId());
							break;
						}
					}
				}
			}
			else {
				for(FieldValue fv: i.getFieldValues()) {
					if (fv instanceof ObjectFieldValue) {
						ObjectFieldValue ofv = (ObjectFieldValue) fv;
						if (target.get(ofv.getInstanceId())) {
							report(i, "." + fv.getField().getName(), ofv.getInstanceId());
							break;
						}
					}
				}
			}
		}
		target = inbound;
		inbound = new RefSet();
	}

	private void report(Instance i, String name, long instanceId) {
		marked.set(i.getInstanceId(), true);
		inbound.set(i.getInstanceId(), true);
		if (gcRoots.get(i.getInstanceId())) {
			System.out.println("#" + instanceId + " <- #" + i.getInstanceId() + name + " " + i.getJavaClass().getName() + " (GCRoot)");
		}
		else {
			System.out.println("#" + instanceId + " <- #" + i.getInstanceId() + name + " " + i.getJavaClass().getName());
		}
	}	
}
