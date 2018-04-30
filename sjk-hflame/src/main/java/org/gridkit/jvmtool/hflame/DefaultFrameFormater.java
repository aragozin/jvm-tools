package org.gridkit.jvmtool.hflame;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;

public class DefaultFrameFormater implements FrameFormater {

	public String toString(GenericStackElement element) {
		String str = element.toString();
		int ch = str.indexOf('(');
		if (ch >= 0) {
			str = str.substring(0, ch);
		}
		return str;
	}
}
