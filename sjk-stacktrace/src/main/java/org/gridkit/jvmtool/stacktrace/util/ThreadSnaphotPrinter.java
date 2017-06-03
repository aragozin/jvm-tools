package org.gridkit.jvmtool.stacktrace.util;

import java.text.SimpleDateFormat;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;

public class ThreadSnaphotPrinter {

	private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	public String format(ThreadSnapshotEvent e) {
		StringBuilder sb = new StringBuilder();
		format(sb, e);
		return sb.toString();
	}
	
	public void format(StringBuilder sb, ThreadSnapshotEvent e) {
        String timestamp = fmt.format(e.timestamp());
        sb
            .append("Thread [")
            .append(e.threadId())
            .append("] ");
        
        if (e.threadState() != null) {
            sb.append(e.threadState()).append(' ');
        }
        
        sb.append("at ").append(timestamp);
        
        if (e.threadName() != null) {
            sb.append(" - ").append(e.threadName());
        }
        sb.append("\n");
        
        StackFrameList trace = e.stackTrace();
        for(StackFrame frame: trace) {
            sb.append(frame).append("\n");
        }
	}
}
