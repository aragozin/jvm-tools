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
package org.gridkit.jvmtool.parser.jstack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.Thread.State;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameArray;

public class JStackDumpParser {

	private TimeZone timeZone;
	private boolean valid;	
	private long dumpTimestamp;
	private String jvmVersion;
	
	private List<ThreadSnapshotEvent> traces = new ArrayList<ThreadSnapshotEvent>();
	private List<String> unparsed = new ArrayList<String>();
	private IOException ioerror;

	private Matcher threadLine = Pattern.compile("\\\"(.*)\\\"\\s+(daemon)?.+tid=0x([0-9a-fA-F]+)\\s+nid=0x([0-9a-fA-F]+)\\s+([^\\[]*)(?:\\[0x([a-fA-F0-9]*)\\])?").matcher("");
    private Matcher threadState = Pattern.compile("\\s+ java\\.lang\\.Thread\\.State:\\s+([A-Z_]+)(?:\\s+\\((.*)\\))?").matcher("");

	
	public JStackDumpParser(Reader source) {
		this(TimeZone.getDefault(), source);
	}
	
	public JStackDumpParser(TimeZone tz, Reader source) {
		BufferedReader br = reader(source);
		this.timeZone = tz;
		loadHeader(br);
		if (valid) {
			parseThreads(br);
		}
	}	
	
	private BufferedReader reader(Reader source) {		
		return (source instanceof BufferedReader) ? ((BufferedReader)source) : new BufferedReader(source);
	}

	private void loadHeader(BufferedReader br) {
		try {
			String line1 = br.readLine();
			String line2 = br.readLine();
			if (line1 != null && line2 != null && line2.startsWith("Full thread dump ")) {
				jvmVersion = line2.substring("Full thread dump ".length());
				
				SimpleDateFormat sdm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				sdm.setTimeZone(timeZone);
				
				Date date = sdm.parse(line1);
				dumpTimestamp = date.getTime();
				
				valid = true;
			}
		} catch (IOException e) {
			// ignore
		} catch (ParseException e) {
			// ignore
		}
	}

	private void parseThreads(BufferedReader br) {
		try {
			while(true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				if (line.trim().length() == 0) {
					continue;
				}
				
				threadLine.reset(line);
				if (threadLine.matches()) {
					readThread(br);
				}
				else {
					unparsed.add(line);
				}
			}
		} catch (IOException e) {
			ioerror = e;
			return;
		}		
	}
	
	private void readThread(BufferedReader reader) throws IOException {
		String threadName = threadLine.group(1);
		boolean daemon = threadLine.group(2) != null;
		long tid = Long.parseLong(threadLine.group(3), 16);
		long nid = Long.parseLong(threadLine.group(4), 16);
		String threadMode = threadLine.group(5).trim();
		String semaphore = threadLine.group(6);
		
		ThreadSnapshotEventPojo pojo = new ThreadSnapshotEventPojo();
		pojo.timestamp(dumpTimestamp);
		pojo.threadName(threadName);
		pojo.threadId(tid);
		pojo.tags().put("jstack.threadState", threadMode);
		if (semaphore != null) {
			pojo.tags().put("jstack.threadSemaphore", semaphore);
		}
		pojo.counters().set("jstack.nid", nid);
		if (daemon) {
			pojo.tags().put("jstack.threadIsDaemon", "true");
		}
		
		traces.add(pojo);
		
		List<StackFrame> frames = new ArrayList<StackFrame>();
		String line = reader.readLine();
		if (line == null) {
			return;
		}
		threadState.reset(line);
		if (threadState.matches()) {
			String jstate = threadState.group(1);
			String jstateExtra = threadState.group(2);
			pojo.threadState(State.valueOf(jstate));
			if (jstateExtra != null) {
				pojo.tags().put("jstack.threadStateExtra", jstateExtra);
			}
			line = reader.readLine();
		}
		
		while(true) {
			if (line == null || line.trim().length() == 0) {
				StackFrameArray sfa = new StackFrameArray(frames);
				pojo.stackTrace(sfa);
				return;
			}
			
			line = line.trim();
			if (line.startsWith("at ")) {
				String fr = line.substring(3);
				frames.add(StackFrame.parseFrame(fr));
			}
			else if (line.startsWith("-")) {
				// ignore
			}
			else {
				unparsed.add(line);
			}
			
			line = reader.readLine();
		}
	}

	public boolean isValid() {
		return valid;
	}
	
	public long getTimestamp() {
		return dumpTimestamp;
	}
	
	public String getJvmDetails() {
		return jvmVersion;
	}
	
	public List<ThreadSnapshotEvent> getThreads() {
		return traces;
	}
	
	public List<String> getUnparsedContent() {
		return unparsed;
	}
	
	public IOException getParseException() {
		return ioerror;
	}
}
