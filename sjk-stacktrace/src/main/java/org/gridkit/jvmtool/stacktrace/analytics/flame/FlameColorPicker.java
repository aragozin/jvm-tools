package org.gridkit.jvmtool.stacktrace.analytics.flame;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;

public interface FlameColorPicker {

    public int pickColor(GenericStackElement[] trace);

}
