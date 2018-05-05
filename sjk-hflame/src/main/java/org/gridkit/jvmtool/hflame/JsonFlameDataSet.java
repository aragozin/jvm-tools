/**
 * Copyright 2018 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.hflame;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.stacktrace.StackFrameList;

public class JsonFlameDataSet {

	private final Map<String, Integer> frameSet = new LinkedHashMap<String, Integer>();
	private final Map<String, ThreadDump> threads = new TreeMap<String, ThreadDump>();
	private final List<String> pallete = new ArrayList<String>();
	private FrameColorChooser colorer;
	private FrameFormater formater;
	
	public JsonFlameDataSet() {
		this.colorer = new DefaultFrameColorChooser();
		this.formater = new DefaultFrameFormater();
		intern(State.RUNNABLE);
		intern(State.BLOCKED);
		intern(State.WAITING);
		intern(State.TIMED_WAITING);
	}
	
	private int intern(String frame) {
		if (frameSet.containsKey(frame)) {
			return frameSet.get(frame);
		}
		else {
			int n = frameSet.size();
			frameSet.put(frame, n);
			pallete.add(toColorLiteral(colorer.getFrameColor(frame)));
			return n;
		}
	}

	private int intern(State state) {
		switch (state) {
		case BLOCKED:
			return intern("(BLOCKED)");
		case NEW:
			return intern("(NEW)");
		case RUNNABLE:
			return intern("(RUNNABLE)");
		case TERMINATED:
			return intern("(TERMINATED)");
		case TIMED_WAITING:
			return intern("(TIMED_WAITING)");
		case WAITING:
			return intern("(WAITING)");
		default:
			return intern("(???)");
		}
	}
	
	private String toColorLiteral(int color) {
		if (color < 0) {
			return "null";
		}
		else {
			String hex = Integer.toHexString(color + 0x1000000);
			hex = hex.substring(hex.length() - 6);
			return "\"#" + hex + "\""; 
		}
	}

	public void feed(Iterable<ThreadSnapshotEvent> events) {
		for(ThreadSnapshotEvent e: events) {
			if (e.stackTrace() != null && e.stackTrace().depth() > 0) {
				String threadName = String.valueOf(e.threadName());
				
				ThreadDump dump = thread(threadName);
				
				int[] trace = intern(e);
				dump.count(trace);
			}
		}
	}
	
	private int[] intern(ThreadSnapshotEvent e) {
		StackFrameList trace = e.stackTrace();
		int[] r = new int[trace.depth() + 1];
		for(int i = 0; i != trace.depth(); ++i) {
			int fp = trace.depth() - 1 - i;
			r[i] = intern(formater.toString(trace.frameAt(fp)));
		}
		
		if (e.threadState() == null) {
//			r = Arrays.copyOf(r, r.length - 1);
			r[r.length - 1] = intern("(???)");
		}
		else {
			r[r.length - 1] = intern(e.threadState());
		}
		
		return r;
	}

	ThreadDump thread(String name) {
		ThreadDump dump = threads.get(name);
		if (dump == null) {
			dump = new ThreadDump(name);
			threads.put(name, dump);
		}
		return dump;
	}
 	
	public void exportJson(StringBuilder sb) {
		sb.append("{");
		sb.append("frames: ");
		exportFrames(sb);
		sb.append(", ");
		sb.append("frameColors: ");
		exportFrameColors(sb);
		sb.append(", ");
		sb.append("threads: ");
		exportThreads(sb);
		sb.append("}");
	}
	
	private void exportFrames(StringBuilder sb) {
		sb.append("[");
		for(String frame: frameSet.keySet()) {
			sb.append('"').append(frame).append("\", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append("]");		
	}

	private void exportFrameColors(StringBuilder sb) {
		sb.append("[");
		for(String col: pallete) {
			sb.append(col).append(", ");
		}
		sb.setLength(sb.length() - 2);
		sb.append("]");		
	}

	private void exportThreads(StringBuilder sb) {
		sb.append("[");
		for(ThreadDump td: threads.values()) {
			sb.append("{ name: \"").append(escape(td.threadName)).append("\", traces: [");
			for(TraceWeight tw: td.traces) {
				sb.append("{ trace: ")
				.append(Arrays.toString(tw.trace))
				.append(", samples: ")
				.append(tw.samples)
				.append("}, ");
			}
			sb.setLength(sb.length() - 2);
			sb.append("]}, ");		
		}
		sb.setLength(sb.length() - 2);
		sb.append("]");		
	}

	private String escape(String threadName) {
		if (threadName.indexOf('\\') >= 0) {
			threadName = threadName.replace("\\", "\\\\");
		}
		if (threadName.indexOf('"') >= 0) {
			threadName = threadName.replace("\"", "\\\"");
		}
		return threadName;
	}

	static class ThreadDump {
		
		public final String threadName;
		public final List<TraceWeight> traces;
		
		public ThreadDump(String name) {
			this.threadName = name;
			this.traces = new ArrayList<TraceWeight>();
		}

		public void count(int[] trace) {
			for(TraceWeight t: traces) {
				if (Arrays.equals(t.trace, trace)) {
					t.samples++;
					return;
				}
			}
			TraceWeight tw = new TraceWeight(trace, 1);
			traces.add(tw);
		}
	}
	
	static class TraceWeight {
		
		public final int[] trace;
		public int samples;
		
		public TraceWeight(int[] trace, int samples) {
			this.trace = trace;
			this.samples = samples;
		}
	}
}
