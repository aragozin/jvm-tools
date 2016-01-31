/**
 * Copyright 2016 Alexey Ragozin
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
package org.gridkit.jvmtool.stacktrace.analytics;

import java.lang.Thread.State;

import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameArray;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.analytics.FlameGraph.ColorPicker;
import org.gridkit.jvmtool.stacktrace.analytics.FlameGraph.SimpleColorPicker;

/**
 * {@link ColorPicker} for {@link FlameGraph}. Rainbow color picker
 * choose coloring hue based on category of trace above color element.
 * <br/>
 * Categories are provided as a list of filters. 
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RainbowColorPicker implements ColorPicker {

    ThreadSnapshotFilter[] filters;
    int[] hues;
    int dh;
    
    public RainbowColorPicker(ThreadSnapshotFilter[] filters) {
        this.filters = filters;
        this.hues = new int[filters.length];
        int lim = Math.min(220, 60 * filters.length);
        int d = lim / (filters.length);
        dh = Math.min(7, d / 3);
        for(int n = 0; n != filters.length; ++n) {
            hues[n] = 12 + n * d;
        }
    }

    @Override
    public int pickColor(StackFrame[] trace) {
        if (trace == null || trace.length < 1) {
            return 0;
        }
        TSnap snap = new TSnap(trace);
        StackFrame frame = trace[trace.length - 1];
        
        for(int n = 0; n != filters.length; ++n) {
            if (filters[n].evaluate(snap)) {
                return SimpleColorPicker.hashColor(hues[n], 0, frame);
            }
        }
        return SimpleColorPicker.hashGrayColor(frame);
    }
    
    
    private static class TSnap implements ThreadSnapshot {

        StackFrameList trace;
        
        public TSnap(StackFrame[] trace) {
            this.trace = new StackFrameArray(trace);
        }
        
        @Override
        public long threadId() {
            return 0;
        }

        @Override
        public String threadName() {
            return null;
        }

        @Override
        public long timestamp() {
            return 0;
        }

        @Override
        public StackFrameList stackTrace() {
            return trace;
        }

        @Override
        public State threadState() {
            return null;
        }

        @Override
        public CounterCollection counters() {
            return null;
        }
    }
}
