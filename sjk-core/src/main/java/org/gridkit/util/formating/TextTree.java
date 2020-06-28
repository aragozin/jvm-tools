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
package org.gridkit.util.formating;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * Helper class for dumping trees. Used for diagnostic
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class TextTree {

    public static TextTree t(String text, TextTree... children) {
        return new TextTree(text, children);
    }

    private String text;
    private TextTree[] children;

    public TextTree(String text, TextTree... children) {
        this.text = text;
        this.children = children;
    }

    public String printAsTree() {
        try {
            StringWriter sw = new StringWriter();
            printTreeNode(sw, "", this, false);
            return sw.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printAsTree(Appendable stream) throws IOException {
        printTreeNode(stream, "", this, false);
    }

    public void printAsTree(Appendable stream, boolean compact) throws IOException {
        printTreeNode(stream, "", this, compact);
    }

    private static void printTreeNode(Appendable sb, String prefix, TextTree node, boolean compact) throws IOException {
        String t = String.valueOf(node.text);
        sb.append(t);
        if (node.children.length == 0) {
            sb.append('\n');
        }
        else {
            String npref = prefix + blank(t.length()) + (!compact ? "| ": "|");
            String cpref = prefix + blank(t.length());
            if (node.children.length == 1) {
                sb.append(!compact ? "--" : "-");
                npref = cpref + (!compact ? "  " : " ");
            }
            else {
                sb.append(!compact ? "+-" : "+");
            }
            printTreeNode(sb, npref , node.children[0], compact);
            for(int i = 1; i < node.children.length; ++i) {
                sb.append(cpref);
                if (i < node.children.length - 1) {
                    sb.append(!compact ? "+-" : "+");
                    printTreeNode(sb, npref , node.children[i], compact);
                }
                else {
                    sb.append(!compact ? "\\-" : "\\");
                    printTreeNode(sb, cpref + (!compact ? "  " : " ") , node.children[i], compact);
                }
            }
        }
    }

    private static String blank(int n) {
        char[] ch = new char[n];
        Arrays.fill(ch, ' ');
        return new String(ch);
    }

    @Override
    public String toString() {
        return text + (children.length == 0 ? "" : Arrays.toString(children));
    }
}
