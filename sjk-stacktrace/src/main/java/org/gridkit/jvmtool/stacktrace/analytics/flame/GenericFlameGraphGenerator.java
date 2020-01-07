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

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.gridkit.jvmtool.codec.stacktrace.ThreadSnapshotEvent;
import org.gridkit.jvmtool.stacktrace.GenericStackElement;
import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;
import org.gridkit.jvmtool.stacktrace.analytics.Calculators;
import org.gridkit.jvmtool.stacktrace.analytics.WeigthCalculator;
import org.gridkit.jvmtool.stacktrace.analytics.WeigthCalculator.CaptionFlavour;

public abstract class GenericFlameGraphGenerator {

    private static final GenericStackElement[] ROOT = new GenericStackElement[0];
    private static final Locale SVG_LOCALE;
    static {
        SVG_LOCALE = Locale.ROOT;
    }

    private Node root;

    private FlameColorPicker colorPicker = new DefaultColorPicker();
    private WeigthCalculator calculator = Calculators.SAMPLES;

    public GenericFlameGraphGenerator() {
        root = new Node(ROOT, comparator());
    }

    public GenericFlameGraphGenerator(WeigthCalculator calculator) {
        root = new Node(ROOT, comparator());
        this.calculator = calculator;
    }

    protected abstract Comparator<GenericStackElement> comparator();

    public void setColorPicker(FlameColorPicker cp) {
        this.colorPicker = cp;
    }

    public void feed(ThreadSnapshotEvent event) {
        Node node = root;
        long w = calculator.getWeigth(event);
        node.totalCount += w;
        StackFrameList trace = event.stackTrace();
        for(int i = trace.depth(); i > 0; --i) {
            StackFrame f = trace.frameAt(i - 1).withoutSource();
            Node c = node.child(f);
            c.totalCount += w;
            node = c;
        }
        node.terminalCount += w;
    }

    public void renderSVG(String title, int width, Writer writer) throws IOException {
        int topm = 24;
        int bm = 0;
        int frameheight = 16;

        int threashold = (int)(1.5d * root.totalCount / width);
        int maxDepth = calculateMaxDepth(root, threashold);

        int height = maxDepth * frameheight + topm + bm;

        appendHeader(width, height, writer);

        format(writer, "<rect x=\"0.0\" y=\"0\" width=\"%d\" height=\"%d\" fill=\"url(#background)\"/>\n", width, height);

        format(writer, "<text text-anchor=\"middle\" x=\"%d\" y=\"%d\" font-size=\"17\" font-family=\"Verdana\" fill=\"rgb(0,0,0)\"  >%s</text>\n", width/2, topm, title);

        appendChildNodes(writer, root, 0, width, height - frameheight, frameheight, threashold);

        format(writer, "</svg>");
    }

    private int calculateMaxDepth(Node node, int threshold) {
        if (node.totalCount < threshold) {
            return 0;
        }
        else {
            int max = 0;
            for(Node n: node.children.values()) {
                max = Math.max(max, calculateMaxDepth(n, threshold));
            }
            return max + 1;
        }
    }

    private void appendChildNodes(Writer writer, Node node, int xoffs, int width, int height, int frameheight, int threshold) throws IOException {
        int x = xoffs;
//        x += node.terminalCount / 2;
        for(Node child: node.children.values()) {
            if (child.totalCount > threshold) {
                renderNode(writer, child, x, height, width, frameheight);
                appendChildNodes(writer, child, x, width, height - frameheight, frameheight, threshold);
            }
            x += child.totalCount;
        }
        if (node.terminalCount > threshold) {
            renderSmoke(writer, x, node.terminalCount, height, width, frameheight);
        }
    }

    private void renderNode(Writer writer, Node node, int x, int height, int width, int frameheight) throws IOException {
        double rx = (double)(width) * x / root.totalCount;
        double rw = (double)(width) * node.totalCount / root.totalCount;
        double ry = height;
        double rh = frameheight;

        int c = colorPicker.pickColor(node.path);
        int cr = (0xFF) & (c >> 16);
        int cg = (0xFF) & (c >>  8);
        int cb = (0xFF) & (c >>  0);

        // Node box
        format(writer, "<g class=\"fbar\">\n");
        format(writer, "<title>%s (%s, %.2f%%)</title>\n",
                escape(describe(node)), formatWeight(node.totalCount), 100d * node.totalCount / root.totalCount);
        format(writer, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"rgb(%d,%d,%d)\" rx=\"2\" ry=\"2\"/>\n",
                rx, ry, rw, rh, cr, cg, cb);
        format(writer, "<text x=\"%.1f\" y=\"%.1f\" fill=\"rgb(0,0,0)\">%s</text>\n",
                rx + 10, ry + frameheight - 3, escape(trimStr(describe(node), (int)(rw - 10) / 7)));
        format(writer, "</g>\n");
    }

    private void renderSmoke(Writer writer, int x, long weight, int height, int width, int frameheight) throws IOException {
        double rx = (double)(width) * x / root.totalCount;
        double rw = (double)(width) * weight / root.totalCount;
        double ry = height;
        double rh = frameheight;

        format(writer, "<g>\n");
        format(writer, "<title>%s, %.2f%%</title>", formatWeight(weight), 100d * weight / root.totalCount);
        format(writer, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"rgb(20,20,20)\" rx=\"1\" ry=\"1\"/>\n",
                rx, ry + rh / 2, rw, 3f);
        format(writer, "</g>\n");
    }

    private Object formatWeight(long totalCount) {
        return calculator.formatWeight(CaptionFlavour.COMMON, totalCount);
    }

    private String trimStr(String describe, int len) {
        if (len < 3) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int n = Math.min(describe.length(), len);
        boolean trimed = describe.length() > len;
        for(int i = 0; i != n; ++i) {
            if (trimed && i > n - 3) {
                sb.append('.');
            }
            else {
                sb.append(describe.charAt(i));
            }
        }
        return sb.toString();
    }

    protected String escape(String text) {
        return text
             .replace((CharSequence)"<", "&lt;")
             .replace((CharSequence)">", "&gt;");
    }

    protected abstract String describe(Node node);

    private void format(Writer writer, String format, Object...  args) throws IOException {
        writer.append(String.format(SVG_LOCALE, format, args));
    }

    protected void appendHeader(int width, int height, Writer writer) throws IOException {
        format(writer, "<?xml version=\"1.0\" standalone=\"no\"?>\n");
        format(writer, "<svg version=\"1.1\" width=\"%d\" height=\"%d\" onload=\"init(evt)\" viewBox=\"0 0 %d %d\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n"
                , width, height, width, height);
        format(writer, "<defs>");
        format(writer, "  <linearGradient id=\"background\" y1=\"0\" y2=\"1\" x1=\"0\" x2=\"0\">\n");
        format(writer, "      <stop stop-color=\"#eeeeee\" offset=\"5%%\"/>\n");
        format(writer, "      <stop stop-color=\"#eeeeb0\" offset=\"95%%\"/>\n");
        format(writer, "  </linearGradient>\n");
        format(writer, "</defs>\n");
        format(writer, "<style type=\"text/css\">\n");
        format(writer, "  text { font-size:12px; font-family:Verdana }\n");
        format(writer, "  .fbar:hover { stroke:black; stroke-width:0.5; cursor:pointer; }\n");
        format(writer, "</style>\n");
    }

    static class Node {

        GenericStackElement[] path;
        long totalCount;
        long terminalCount;
        Comparator<GenericStackElement> comparator;
        SortedMap<GenericStackElement, Node> children;

        public Node(GenericStackElement[] path, Comparator<GenericStackElement> comparator) {
            this.path = path;
            this.comparator = comparator;
            this.children = new TreeMap<GenericStackElement, Node>(comparator);
        }

        public Node child(GenericStackElement f) {
            Node c = children.get(f);
            if (c == null) {
                GenericStackElement[] npath = Arrays.copyOf(path, path.length + 1);
                npath[path.length] = f;
                c = new Node(npath, comparator);
                children.put(f, c);
            }
            return c;
        }

        public GenericStackElement element() {
            return path[path.length - 1];
        }

        @Override
        public String toString() {
            return path.length == 0 ? "<root>" : path[path.length - 1].toString();
        }
    }
}
