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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventDumpParser;
import org.gridkit.jvmtool.event.EventMorpher;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.MorphingEventReader;

public class JStackThreadDumpLoader implements EventDumpParser {

	@Override
	public boolean isFunctional() {
		return true;
	}

	@Override
	public EventReader<Event> open(InputStreamSource source) throws IOException {
		InputStream is = source.open();
		Reader reader = new InputStreamReader(is);
		JStackDumpParser parser = new JStackDumpParser(reader);
		try {
			is.close();
		}
		catch(IOException e) {			
		}
		if (parser.isValid()) {
			List<ThreadSnapshotEvent> list = parser.getThreads();
			final Iterator<Event> it = iterator(list);
			return new DumpReader(it).morph(new EventMorpher<Event, Event>() {
				@Override
				public Event morph(Event event) {
					return event;
				}
			});
		}
		else {
			return null;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Iterator<Event> iterator(List<ThreadSnapshotEvent> list) {
		return (Iterator)list.iterator();
	}

	@Override
	public String toString() {		
		return "JStack dump parser";
	}
	
	private static final class DumpReader implements EventReader<Event> {
		
		private final Iterator<Event> it;

		private DumpReader(Iterator<Event> it) {
			this.it = it;
		}

		@Override
		public Iterator<Event> iterator() {
			return it;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public Event next() {
			return it.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <M extends Event> EventReader<M> morph(EventMorpher<Event, M> morpher) {
			return MorphingEventReader.morph(this, morpher);
		}

		@Override
		public Event peekNext() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void dispose() {
		}
	}	
}
