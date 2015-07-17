/**
 * Copyright 2014 Alexey Ragozin
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

package org.gridkit.jvmtool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.util.formating.TextTable;

/**
 * Stack frame histogram.
 *  
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackHisto {

    public static Comparator<SiteInfo> BY_HITS = new HitComparator();
    public static Comparator<SiteInfo> BY_OCCURENCE = new OccurenceComparator();
    
    private Map<StackTraceElement, SiteInfo> histo = new HashMap<StackTraceElement, SiteInfo>();
    private int traceCount = 0;
    
    public void feed(StackTraceElement[] trace) {
        ++traceCount;
        if (trace.length == 0) {
            return;
        }
        Set<StackTraceElement> seen = new HashSet<StackTraceElement>();
        for(StackTraceElement e: trace) {
            SiteInfo si = getSiteInfo(e);
            si.hitCount += 1;
            if (seen.add(si.site)) {
                si.occurences += 1;
            }
        }
        StackTraceElement last = trace[0];
        getSiteInfo(last).terminalCount++;
    }

    protected SiteInfo getSiteInfo(StackTraceElement e) {
        SiteInfo si = histo.get(e);
        if (si == null) {
            si = new SiteInfo();
            si.site = e;
            histo.put(e, si);
        }
        return si;
    }
    
    public String formatHisto() {
        return formatHisto(Integer.MAX_VALUE);
    }

    public String formatHisto(int limit) {
        TextTable tt = new TextTable();
        tt.addRow("Trc N ", "", " Frm N", " Term N ", "", " Frame");
        List<SiteInfo> h = new ArrayList<SiteInfo>(histo.values());
        Collections.sort(h, BY_OCCURENCE);
        int n = 0;
        for(SiteInfo si: h) {            
            String traceN = si.getOccurences() + " ";
            String tracePtc = ((100 * si.getOccurences()) / traceCount) + "%";
            String frameN = " " + si.getHitCount();
            String termN = " " + si.getTerminalCount() + " ";
            String termPtc = ((100 * si.getTerminalCount()) / traceCount) + "%";
            tt.addRow(traceN, tracePtc, frameN, termN, termPtc, " " + si.getSite());
            if (limit <= ++n) {
                break;
            }
        }
        return tt.formatTextTableUnbordered(200);
    }
    
    public static class SiteInfo {
        
        StackTraceElement site;
        int hitCount;
        int occurences;
        int terminalCount;
        
        public StackTraceElement getSite() {
            return site;
        }

        public int getHitCount() {
            return hitCount;
        }
        
        public int getTerminalCount() {
            return terminalCount;
        }

        public int getOccurences() {
            return occurences;
        }
    }
    
    private static class HitComparator implements Comparator<SiteInfo> {

        @Override
        public int compare(SiteInfo o1, SiteInfo o2) {
            return ((Integer)o2.hitCount).compareTo(o1.hitCount);
        }
    }

    private static class OccurenceComparator implements Comparator<SiteInfo> {

        @Override
        public int compare(SiteInfo o1, SiteInfo o2) {
            return ((Integer)o2.occurences).compareTo(o1.occurences);
        }
    }
}
