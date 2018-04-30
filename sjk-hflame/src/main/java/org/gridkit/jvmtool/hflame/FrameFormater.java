package org.gridkit.jvmtool.hflame;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;

public interface FrameFormater {

	public String toString(GenericStackElement element);
}
