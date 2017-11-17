package org.gridkit.jvmtool;

import javax.management.MXBean;

@MXBean(true)
public interface DummyMBean {

	public void callSingleStringArg(String arg);
	
	public void callDoubleStringArg(String arg1, String arg2);
	
	public void callStringArrayArg(String[] args);
	
}
