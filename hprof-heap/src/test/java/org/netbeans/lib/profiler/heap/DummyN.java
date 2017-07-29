package org.netbeans.lib.profiler.heap;

public class DummyN {

	public String dummyName;

	public DummyN(String dummyName) {
		super();
		this.dummyName = dummyName;
	}
	
	public Object newInner(String id) {
		return new MyInnerClass(id);
	}
	
	class MyInnerClass {
	
		public String innerName;

		public MyInnerClass(String innerName) {
			super();
			this.innerName = innerName;
		}
	}		
}
