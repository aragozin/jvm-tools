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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.ThreadRecord;
import org.gridkit.jvmtool.stacktrace.analytics.ThreadSnapshotFilter;
import org.gridkit.util.formating.TextTable;

/**
 * Stack frame histogram.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackHisto {

    public static Comparator<SiteInfo> BY_HITS = new HitComparator();
    public static Comparator<SiteInfo> BY_OCCURENCE = new OccurenceComparator();
    public static Comparator<SiteInfo> BY_TERMINAL = new TerminalComparator();

    private String[] conditionNames = new String[0];
    private ThreadSnapshotFilter[] conditionFilters = new ThreadSnapshotFilter[0];

    private Map<StackFrame, SiteInfo> histo = new HashMap<StackFrame, SiteInfo>();
    private int traceCount = 0;

    private Comparator<SiteInfo> histoOrder = BY_OCCURENCE;

    public void addCondition(String name, ThreadSnapshotFilter filter) {
        int n = conditionNames.length;
        conditionNames = Arrays.copyOf(conditionNames, n + 1);
        conditionFilters = Arrays.copyOf(conditionFilters, n + 1);
        conditionNames[n] = name;
        conditionFilters[n] = filter;
    }

    public void feed(StackFrameList trace) {
        ++traceCount;
        if (trace.depth() == 0) {
            return;
        }
        boolean[] matchVector = matchVector(trace);
        Set<StackFrame> seen = new HashSet<StackFrame>();
        for(StackFrame e: trace) {
            SiteInfo si = getSiteInfo(e);
            si.hitCount += 1;
            if (seen.add(si.site)) {
                si.occurences += 1;
                for(int m = 0; m != matchVector.length; ++m) {
                    if (matchVector[m]) {
                        si.conditionalCounts[m] += 1;
                    }
                }
            }
        }
        StackFrame last = trace.frameAt(0);
        getSiteInfo(last).terminalCount++;
    }

    public void setHistoOrder(Comparator<SiteInfo> comparator) {
	this.histoOrder = comparator;
    }

    private boolean[] matchVector(StackFrameList trace) {
        boolean[] vec = new boolean[conditionNames.length];
        for(int i = 0; i != vec.length; ++i) {
            vec[i] = conditionFilters[i].evaluate(new ThreadRecord(trace));
        }
        return vec;
    }

    protected SiteInfo getSiteInfo(StackFrame e) {
        SiteInfo si = histo.get(e);
        if (si == null) {
            si = new SiteInfo();
            si.site = e;
            si.conditionNames = conditionNames;
            si.conditionalCounts = new int[conditionNames.length];
            histo.put(e, si);
        }
        return si;
    }

    public String formatHisto() {
        return formatHisto(Integer.MAX_VALUE);
    }

    public String formatHistoToCSV() {
        return formatHistoToCSV(Integer.MAX_VALUE);
    }

    public String formatHisto(int limit) {
        TextTable tt = new TextTable();
        List<String> row = new ArrayList<String>();
        row.add("Trc\t(%)");
        row.add("  Frm\t N  ");
        row.add("Term\t(%)");
        for(String name: conditionNames) {
            String cf = "  [\t" + name + "\t]";
            row.add(cf);
        }
        row.add("  Frame");
        tt.addRow(row);
        List<SiteInfo> h = new ArrayList<SiteInfo>(histo.values());
        Collections.sort(h, histoOrder);
        int n = 0;
        for(SiteInfo si: h) {
            row.clear();
            String traceN = "\t" + si.getOccurences() + " " + formatPct(si.getOccurences(), traceCount);
            String frameN = "  " + si.getHitCount() + "  ";
            String termN = "\t" + si.getTerminalCount() + " " + formatPct(si.getTerminalCount(),traceCount);
            row.add(traceN);
            row.add(frameN);
            row.add(termN);
            for(String name: conditionNames) {
                String cf = "  \t" + si.getConditionalCount(name) + " " + formatPct(si.getConditionalCount(name), si.getOccurences());
                row.add(cf);
            }
            row.add("  " + si.getSite());
            tt.addRow(row);
            if (limit <= ++n) {
                break;
            }
        }
        return tt.formatTextTableUnbordered(200);
    }

    public String formatHistoToCSV(int limit) {
        TextTable tt = new TextTable();
        List<String> row = new ArrayList<String>();
        row.add("Trace N");
        row.add("Frame N");
        row.add("Terminal N");
        for(String name: conditionNames) {
            String cf = "[" + name + "]";
            row.add(cf);
        }
        row.add("Frame");
        tt.addRow(row);
        List<SiteInfo> h = new ArrayList<SiteInfo>(histo.values());
        Collections.sort(h, histoOrder);
        int n = 0;
        for(SiteInfo si: h) {
            row.clear();
            String traceN = "" + si.getOccurences();
            String frameN = "" +si.getHitCount();
            String termN = "" + si.getTerminalCount();
            row.add(traceN);
            row.add(frameN);
            row.add(termN);
            for(String name: conditionNames) {
                String cf = "" + si.getConditionalCount(name);
                row.add(cf);
            }
            row.add("" + si.getSite());
            tt.addRow(row);
            if (limit <= ++n) {
                break;
            }
        }
        return TextTable.formatCsv(tt);
    }

    private String formatPct(int num, int denom) {
        return String.format("%3d%%", (100 * num)/denom);
    }

    public static class SiteInfo {

        StackFrame site;
        int hitCount;
        int occurences;
        int terminalCount;

        String[] conditionNames;
        int[] conditionalCounts;

        public StackFrame getSite() {
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

        public int getConditionalCount(String condition) {
            if (conditionNames == null) {
                return 0;
            }
            for(int i = 0; i != conditionNames.length; ++i) {
                if (condition.equals(conditionNames[i])) {
                    return conditionalCounts[i];
                }
            }
            return 0;
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

    private static class TerminalComparator implements Comparator<SiteInfo> {

	@Override
	public int compare(SiteInfo o1, SiteInfo o2) {
		int term1 = o1.terminalCount;
		int term2 = o2.terminalCount;
		int c = ((Integer)term2).compareTo(term1);
		return c != 0 ? c : ((Integer)o2.occurences).compareTo(o1.occurences);
	}
    }
}
