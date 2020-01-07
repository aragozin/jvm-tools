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
package org.gridkit.jvmtool.stacktrace.analytics.flame;

import java.lang.Thread.State;

import org.gridkit.jvmtool.event.TagCollection;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.GenericStackElement;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameArray;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;

/**
 * {@link FlameColorPicker} for {@link FlameGraphGenerator}. Rainbow color picker
 * choose coloring hue based on category of trace above color element.
 * <br/>
 * Categories are provided as a list of filters.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RainbowColorPicker implements FlameColorPicker {

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
    public int pickColor(GenericStackElement[] trace) {
        if (trace == null || trace.length < 1) {
            return 0;
        }
        StackFrame[] ftrace = new StackFrame[trace.length];
        for(int i = 0; i != trace.length; ++i) {
            ftrace[i] = (StackFrame) trace[i];
        }
        TSnap snap = new TSnap(ftrace);
        StackFrame frame = ftrace[ftrace.length - 1];

        for(int n = 0; n != filters.length; ++n) {
            if (filters[n].evaluate(snap)) {
                return DefaultColorPicker.hashColor(hues[n], 0, frame);
            }
        }
        return DefaultColorPicker.hashGrayColor(frame);
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

        @Override
        public TagCollection tags() {
            return null;
        }
    }
}
