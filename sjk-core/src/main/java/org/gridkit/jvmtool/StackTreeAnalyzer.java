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

import static org.gridkit.util.formating.TextTree.t;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.util.formating.TextTree;

/**
 * Stack tree analyzis helper.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class StackTreeAnalyzer {

    private final static StackTraceElement STUB = new StackTraceElement("", "", null, -1);

    private Node root = new Node();

    private int maxDepth = Integer.MAX_VALUE;
    private boolean trimLineNumbers = false;
    private boolean compressedTree = false;
    private double branchVisibilityRelativeThreshold = 0;
    private double branchVisibilityAbsoluteThreshold = 0;
    private boolean showSkeletonTails = false;

    private StringBuilder maskBuilder;
    private Pattern maskPattern;

    private StringBuilder classLumpBuilder;
    private Pattern classLumpPattern;

    private StringBuilder skeletonBuilder;
    private Pattern skeletonPattern;

    private StringBuilder tipBuilder;
    private Pattern tipPattern;


    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setTrimLineNumbers(boolean trim) {
        this.trimLineNumbers = trim;
    }

    public void setCompressedTree(boolean compress) {
        this.compressedTree = compress;
    }

    public void setRelativeVisibilityThreshold(double threshold) {
        this.branchVisibilityRelativeThreshold = threshold;
    }

    public void setAbsoluteVisibilityThreshold(double threshold) {
        this.branchVisibilityAbsoluteThreshold = threshold;
    }

    public void setShowSkeletonTails(boolean show) {
        this.showSkeletonTails = show;
    }

    public void mask(String pattern) {
        Pattern p = GlobHelper.translate(pattern, ".");
        maskPattern = null;
        if (maskBuilder == null) {
            maskBuilder = new StringBuilder();
        }
        else {
            maskBuilder.append("|");
        }
        maskBuilder.append("(").append(p.pattern()).append(")");
    }

    public void lumpClass(String pattern) {
        Pattern p = GlobHelper.translate(pattern, ".");
        classLumpPattern = null;
        if (classLumpBuilder == null) {
            classLumpBuilder = new StringBuilder();
        }
        else {
            classLumpBuilder.append("|");
        }
        classLumpBuilder.append("(").append(p.pattern()).append(")");
    }

    public void retain(String pattern) {
        Pattern p = GlobHelper.translate(pattern, ".");
        skeletonPattern = null;
        if (skeletonBuilder == null) {
            skeletonBuilder = new StringBuilder();
        }
        else {
            skeletonBuilder.append("|");
        }
        skeletonBuilder.append("(").append(p.pattern()).append(")");
    }

    public void tip(String pattern) {
        Pattern p = GlobHelper.translate(pattern, ".");
        tipPattern = null;
        if (tipBuilder == null) {
            tipBuilder = new StringBuilder();
        }
        else {
            tipBuilder.append("|");
        }
        tipBuilder.append("(").append(p.pattern()).append(")");
    }

    public void feed(StackTraceElement[] trace) {
        ensureConfigured();
        StackTraceElement[] rtrace = trace;
        if (tipPattern != null) {
            rtrace = trimTips(rtrace);
        }
        if (classLumpPattern != null) {
            rtrace = lumpClasses(rtrace);
        }
        if (skeletonPattern != null) {
            rtrace = strip(rtrace);
        }
        if (maskPattern != null) {
            rtrace = mask(rtrace);
        }
        if (trimLineNumbers) {
            rtrace = trimNumbers(rtrace);
        }
        rtrace = trim(rtrace, maxDepth);
        append(root, rtrace, rtrace.length);
    }

    private void ensureConfigured() {
        if (maskBuilder != null && maskPattern == null) {
            maskPattern = Pattern.compile(maskBuilder.toString());
        }
        if (skeletonBuilder != null && skeletonPattern == null) {
            skeletonPattern = Pattern.compile(skeletonBuilder.toString());
        }
        if (tipBuilder != null && tipPattern == null) {
            tipPattern = Pattern.compile(tipBuilder.toString());
        }
        if (classLumpBuilder != null && classLumpPattern == null) {
            classLumpPattern = Pattern.compile(classLumpBuilder.toString());
        }
    }

    private StackTraceElement[] trim(StackTraceElement[] rtrace, int maxDepth) {
        if (rtrace.length <= maxDepth) {
            return rtrace;
        }
        else {
            return Arrays.copyOfRange(rtrace, rtrace.length - maxDepth, rtrace.length);
        }
    }

    private StackTraceElement[] mask(StackTraceElement[] rtrace) {
        List<StackTraceElement> ftrace = new ArrayList<StackTraceElement>();
        boolean masked = false;
        for(StackTraceElement e: rtrace) {
            String frame = toShortFrame(e);
            Matcher m = maskPattern.matcher(frame);
            if (m.matches()) {
                if (masked) {
                    continue;
                }
                else {
                    ftrace.add(STUB);
                    masked = true;
                }
            }
            else {
                ftrace.add(e);
                masked = false;
            }
        }
        return ftrace.toArray(new StackTraceElement[ftrace.size()]);
    }

    private StackTraceElement[] lumpClasses(StackTraceElement[] rtrace) {
        List<StackTraceElement> ftrace = new ArrayList<StackTraceElement>();
        for(StackTraceElement e: rtrace) {
            String cn = e.getClassName();
            Matcher m = classLumpPattern.matcher(cn);
            if (ftrace.size() > 0 && m.matches()) {
                StackTraceElement prev = ftrace.get(ftrace.size() - 1);
                if (cn.equals(prev.getClassName())) {
                    ftrace.remove(ftrace.size() - 1);
                }
            }
            ftrace.add(e);
        }
        return ftrace.toArray(new StackTraceElement[ftrace.size()]);
    }

    private StackTraceElement[] strip(StackTraceElement[] rtrace) {
        List<StackTraceElement> ftrace = new ArrayList<StackTraceElement>();
        boolean matched = false;
        for(StackTraceElement e: rtrace) {
            String frame = toShortFrame(e);
            Matcher m = skeletonPattern.matcher(frame);
            if (m.matches()) {
                matched = true;
                ftrace.add(e);
            }
            else {
                if (showSkeletonTails && !matched) {
                    ftrace.add(e);
                }
            }
        }
        return ftrace.toArray(new StackTraceElement[ftrace.size()]);
    }

    private StackTraceElement[] trimTips(StackTraceElement[] rtrace) {
        List<StackTraceElement> ftrace = new ArrayList<StackTraceElement>();
        boolean matched = false;
        for(StackTraceElement e: rtrace) {
            String frame = toShortFrame(e);
            Matcher m = tipPattern.matcher(frame);
            if (!matched && m.matches()) {
                matched = true;
                ftrace.clear();
                ftrace.add(e);
            }
            else {
                ftrace.add(e);
            }
        }
        return ftrace.toArray(new StackTraceElement[ftrace.size()]);
    }

    private StackTraceElement[] trimNumbers(StackTraceElement[] rtrace) {
        return rtrace;
    }

    public TextTree getTree() {
        return asTree(root);
    }

    private TextTree asTree(Node node) {
        if (compressedTree && node.children.size() == 1) {
            Node nnode = node;
            int n = 0;
            while(nnode.children.size() == 1) {
                Node nn = nnode.children.values().iterator().next();
//                if (nn.hitCount != node.hitCount) {
//                    break;
//                }
                nnode = nn;
                ++n;
            }
            TextTree[] c;
            if (nnode.children.isEmpty()) {
                c = new TextTree[2];
            }
            else {
                c = new TextTree[3];
                c[2] = asTree(nnode);
            }
            c[0] = t("skip " + n + (n == 1 ? " frame" : "frames"));
            c[1] = t("[" + nnode.hitCount + "] " + toString(nnode.element));
            return t("", c);
        }
        else {
            List<Node> children = new ArrayList<Node>();
            children.addAll(node.children.values());
            Collections.sort(children, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return o2.hitCount - o1.hitCount;
                }
            });

            List<TextTree> tt = new ArrayList<TextTree>();

            for(Node n: children) {
                double p = (1d * n.hitCount) / node.hitCount;
                double pa = (1d * n.hitCount) / root.hitCount;
                if (p < branchVisibilityRelativeThreshold || (pa < branchVisibilityAbsoluteThreshold)) {
                    continue;
                }
                String rate = String.format("%.1f%% (%.1f%%)", 100 * p, 100 * pa);
                TextTree[] ttt;
                if (n.children.isEmpty()) {
                    ttt = new TextTree[1];
                }
                else {
                    ttt = new TextTree[2];
                    ttt[1] = asTree(n);
                }
                ttt[0] = t(rate, t("[" + n.hitCount + "] " + toString(n.element)));
                tt.add(TextTree.t("", ttt));
            }

            return new TextTree("", tt.toArray(new TextTree[tt.size()]));
        }
    }

    private void append(Node node, StackTraceElement[] trace, int pos) {
        ++node.hitCount;
        if (pos != 0) {
            StackTraceElement e = trace[pos - 1];
            Node c = node.children.get(e);
            if (c == null) {
                c = new Node();
                c.element = e;
                c.parent = node;
                node.children.put(e, c);
            }
            append(c, trace, pos - 1);
        }
    }

    private String toString(StackTraceElement ste) {
        if (ste == STUB) {
            return "[...]";
        }
        else if (ste.isNativeMethod() || ste.getLineNumber() < 0) {
            return toShortFrame(ste);
        }
        else {
            return ste.toString();
        }
    }

    private String toShortFrame(StackTraceElement ste) {
        return ste.getClassName() + "." + ste.getMethodName();
    }

    private static class Node {

        @SuppressWarnings("unused")
        Node parent;
        StackTraceElement element;
        int hitCount;
        Map<StackTraceElement, Node> children = new HashMap<StackTraceElement, StackTreeAnalyzer.Node>();

    }
}
