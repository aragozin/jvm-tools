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

import java.io.IOException;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEventPojo;
import org.gridkit.jvmtool.event.UniversalEventWriter;
import org.gridkit.jvmtool.stacktrace.StackTraceWriter;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

class ThreadEventAdapter implements StackTraceWriter {

    final UniversalEventWriter writer;

    public ThreadEventAdapter(UniversalEventWriter writer) {
        this.writer = writer;
    }

    @Override
    public void write(ThreadSnapshot snap) throws IOException {
        ThreadSnapshotEventPojo pojo = new ThreadSnapshotEventPojo();
        pojo.loadFrom(snap);
        writer.store(pojo);
    }

    @Override
    public void close() {
    	try {
    		writer.close();
    	}
    	catch(IOException e) {
    		throw new RuntimeException(e);
    	}
    }
}