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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotExpander;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.ShieldedEventReader;
import org.gridkit.jvmtool.event.UniversalEventWriter;
import org.gridkit.jvmtool.stacktrace.ThreadDumpSampler;
import org.gridkit.jvmtool.stacktrace.ThreadEventCodec;
import org.junit.BeforeClass;
import org.junit.Test;

public class JsonFlameDataSetTest {

	public static String testDump;
	
	@BeforeClass
	public static void initDump() throws FileNotFoundException, IOException {
		testDump = "target/dump/" + System.currentTimeMillis() + ".sjk";
		File f = new File(testDump);
		f.getParentFile().mkdirs();
		
        ThreadDumpSampler sampler = new ThreadDumpSampler();
        sampler.enableThreadStackTrace(true);
        
        sampler.connect(ManagementFactory.getThreadMXBean());
		
        UniversalEventWriter uew = ThreadEventCodec.createEventWriter(new FileOutputStream(f));
        ThreadEventAdapter writer = new ThreadEventAdapter(uew);
        
        for(int i = 0; i != 100; ++i) {
        	sampler.collect(writer);
        }        
        
        uew.close();
	}	
	
	@Test
	public void smoke_flame_dump() throws FileNotFoundException, IOException {
		EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream(testDump));
		EventReader<ThreadSnapshotEvent> traceReader = ShieldedEventReader.shield(reader.morph(new ThreadSnapshotExpander()), ThreadSnapshotEvent.class, true);
		
		JsonFlameDataSet dump = new JsonFlameDataSet();
		
		dump.feed(traceReader);
		
		StringBuilder sb = new StringBuilder();
		dump.exportJson(sb);
		System.out.println(sb.toString());		
	}
	
	@Test
	public void smoke_flame_dump_hz1_visualvm() throws FileNotFoundException, IOException {
		EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream("src/test/resources/hz1_jvisualvm.sjk"));
		EventReader<ThreadSnapshotEvent> traceReader = ShieldedEventReader.shield(reader.morph(new ThreadSnapshotExpander()), ThreadSnapshotEvent.class, true);
		
		JsonFlameDataSet dump = new JsonFlameDataSet();
		
		dump.feed(traceReader);
		
		StringBuilder sb = new StringBuilder();
		dump.exportJson(sb);
		System.out.println(sb.toString());		
	}
	
	@Test
	public void smoke_flame_dump_hz1_stcap() throws FileNotFoundException, IOException {
		
		EventReader<Event> reader = ThreadEventCodec.createEventReader(new FileInputStream("src/test/resources/hz1_dump.sjk"));
		EventReader<ThreadSnapshotEvent> traceReader = ShieldedEventReader.shield(reader.morph(new ThreadSnapshotExpander()), ThreadSnapshotEvent.class, true);
		
		JsonFlameDataSet dump = new JsonFlameDataSet();
		
		dump.feed(traceReader);
		
		StringBuilder sb = new StringBuilder();
		dump.exportJson(sb);
		System.out.println(sb.toString());		
	}
}
