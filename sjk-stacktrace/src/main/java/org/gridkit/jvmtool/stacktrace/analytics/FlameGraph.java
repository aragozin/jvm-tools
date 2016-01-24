package org.gridkit.jvmtool.stacktrace.analytics;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.gridkit.jvmtool.stacktrace.StackFrame;
import org.gridkit.jvmtool.stacktrace.StackFrameList;

public class FlameGraph {

    private static final StackFrame[] ROOT = new StackFrame[0];
    private static final FrameComparator FRAME_COMPARATOR = new FrameComparator();

    private Node root;
    
    private ColorPicker colorPicker = new SimpleColorPicker();    
//    private int barHeight = 16;
    
    public FlameGraph() {
        root = new Node(ROOT);        
    }
    
    public void setColorPicker(ColorPicker cp) {
        this.colorPicker = cp;
    }
    
    public void feed(StackFrameList trace) {
        Node node = root;
        ++node.totalCount;
        for(int i = trace.depth(); i > 0; --i) {
            StackFrame f = trace.frameAt(i - 1).withoutSource();
            Node c = node.child(f);
            ++c.totalCount;
            node = c;
        }
        ++node.terminalCount;
    }
    
    public void renderSVG(String title, int width, Writer writer) throws IOException {
        int topm = 24;
        int bm = 0;
        int frameheight = 16;

        int threashold = (int)(1.5d * root.totalCount / width);
        int maxDepth = calculateMaxDepth(root, threashold);

        int height = maxDepth * frameheight + topm + bm;  
        
        String line = null;

        appendHeader(width, height, writer);
        
        format(writer, "<rect x=\"0.0\" y=\"0\" width=\"%d\" height=\"%d\" fill=\"url(#background)\"/>", width, height);
        writer.append(line).append("\n");

        format(writer, "<text text-anchor=\"middle\" x=\"%d\" y=\"%d\" font-size=\"17\" font-family=\"Verdana\" fill=\"rgb(0,0,0)\"  >%s</text>", width/2, topm, title);
        writer.append(line).append("\n");
        
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
        x += node.terminalCount / 2;
        for(Node child: node.children.values()) {
            if (child.totalCount > threshold) {
                renderNode(writer, child, x, height, width, frameheight);
                appendChildNodes(writer, child, x, width, height - frameheight, frameheight, threshold);
            }
            x += child.totalCount;
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
        format(writer, "<title>%s (%d samples, %.2f%%)</title>\n", 
                describe(node), node.totalCount, 100d * node.totalCount / root.totalCount);
        format(writer, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"rgb(%d,%d,%d)\" rx=\"2\" ry=\"2\"/>\n",
                rx, ry, rw, rh, cr, cg, cb);
        format(writer, "<text text-anchor=\"\" x=\"%.1f\" y=\"%.1f\" fill=\"rgb(0,0,0)\">%s</text>\n",
                rx + 10, ry + frameheight - 3, trimStr(describe(node), (int)(rw - 10) / 7));
        format(writer, "</g>\n");
        
        if (node.terminalCount == node.totalCount) {
            format(writer, "<g class=\"func_g\">\n");
            format(writer, "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"rgb(20,20,20)\" rx=\"1\" ry=\"1\"/>\n",
                    rx, ry - rh / 2, rw, 3f);
            format(writer, "</g>\n");            
        }
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

    private String describe(Node node) {
        StackFrame frame = node.path[node.path.length - 1];
        String line = frame.getClassName() + "." + frame.getMethodName();
        line = line.replace((CharSequence)"<", "&lt;");
        line = line.replace((CharSequence)">", "&gt;");
        return line;
    }

    private void format(Writer writer, String format, Object...  args) throws IOException {
        writer.append(String.format(format, args));
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
    
    private static class Node {
        
        StackFrame[] path;
        int totalCount;
        int terminalCount;
        SortedMap<StackFrame, Node> children = new TreeMap<StackFrame, Node>(FRAME_COMPARATOR);

        public Node(StackFrame[] path) {
            this.path = path;
        }

        public Node child(StackFrame f) {
            Node c = children.get(f);
            if (c == null) {
                StackFrame[] npath = Arrays.copyOf(path, path.length + 1);
                npath[path.length] = f;
                c = new Node(npath);
                children.put(f, c);
            }
            return c;
        }        
        
        @Override
        public String toString() {
            return path.length == 0 ? "<root>" : path[path.length - 1].toString();
        }
    }
    
    private static class FrameComparator implements Comparator<StackFrame> {

        @Override
        public int compare(StackFrame o1, StackFrame o2) {
            int n = compare(o1.getClassName(), o2.getClassName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getLineNumber(), o2.getLineNumber());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getMethodName(), o2.getMethodName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getSourceFile(), o2.getSourceFile());
            return 0;
        }

        private int compare(int n1, int n2) {            
            return Long.signum(((long)n1) - ((long)n2));
        }

        private int compare(String str1, String str2) {
            if (str1 == str2) {
                return 0;
            }
            else if (str1 == null) {
                return -1;
            }
            else if (str2 == null) {
                return 1;
            }
            return str1.compareTo(str2);
        }
    }
    
    public interface ColorPicker {
        
        public int pickColor(StackFrame[] trace);
        
    }
    
    public class SimpleColorPicker implements ColorPicker {

        @Override
        public int pickColor(StackFrame[] trace) {
            
            if (trace.length == 0) {
                return 0xFFFFFF;
            }
            
            StackFrame sf = trace[trace.length - 1];
            
            int hP = packageNameHash(sf.getClassName());
            int hC = classNameHash(sf.getClassName());
            int hM = sf.getMethodName().hashCode();
            
            int hue = 12 + (hP % 20) - 10;
            int sat = 180 + (hC % 20) - 10;
            int lum = 220 + (hM % 20) - 10;
            
            int c = Color.HSBtoRGB(hue / 255f, sat / 255f, lum / 255f);
            
            return c;
        }

        private int packageNameHash(String className) {
            int c = className.lastIndexOf('.');
            if (c >= 0) {
                return className.substring(0, c).hashCode();
            }
            else {
                return 0;
            }
        }

        private int classNameHash(String className) {
            int c = className.lastIndexOf('.');
            if (c >= 0) {
                className = className.substring(c + 1);
            }
            
            c = className.indexOf('$');
            if (c >= 0) {
                int nhash = className.substring(0, c).hashCode();
                int shash = className.substring(c + 1).hashCode();
                return nhash + (shash % 10);
            }
            else {
                return className.hashCode();
            }
        }
    }
}
