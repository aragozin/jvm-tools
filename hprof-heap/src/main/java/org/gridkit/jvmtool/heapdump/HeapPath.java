package org.gridkit.jvmtool.heapdump;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.netbeans.lib.profiler.heap.Instance;

class HeapPath {

    static PathStep[] parsePath(String path, boolean strictPath) {

        List<PathStep> result = new ArrayList<PathStep>();

        if (path.endsWith(".")) {
            throw new IllegalArgumentException("Invalid path spec: " + path);
        }

        boolean fieldRequired = false;
        boolean dotAllowed = false;
        int n = 0;
        while(n < path.length()) {
            String token = token(n, path);
            n += token.length();
            if (token.length() == 0) {
                throw new RuntimeException("Internal error parsing: " + path);
            }
            if (".".equals(token)) {
                if (dotAllowed) {
                    fieldRequired = true;
                    dotAllowed = false;
                }
                else {
                    throw new IllegalArgumentException("Invalid path spec: " + path);
                }
            }
            else if (token.charAt(0) == '(') {
                if (fieldRequired) {
                    throw new IllegalArgumentException("Invalid path spec: " + path);
                }
                String pattern = token.substring(1, token.length() - 1);
                try {
                    TypeFilterStep step = new TypeFilterStep(pattern, true);
                    result.add(step);
                }
                catch(RuntimeException e) {
                    throw new IllegalArgumentException("Invalid path spec: " + path, e);
                }

                dotAllowed = false;
                fieldRequired = false;
                continue;
            }
            else if (token.charAt(0) == '[') {
                if (fieldRequired) {
                    throw new IllegalArgumentException("Invalid path spec: " + path);
                }
                String index = token.substring(1, token.length() - 1).trim();
                if (index.equals("*")) {
                    result.add(new ArrayIndexStep(-1));
                }
                else {
                    try {
                        int ai = Integer.valueOf(index);
                        result.add(new ArrayIndexStep(ai));
                    }
                    catch(NumberFormatException e) {
                        // try to parse predicate
                        int c = index.lastIndexOf('=');
                        if (c > 0) {
                            String subpath = index.substring(0, c);
                            String matcher = index.substring(c + 1, index.length());
                            PathStep[] steps = parsePath(subpath, true);
                            result.add(new PredicateStep(steps, matcher));
                        }
                        else {
                            throw new IllegalArgumentException("Invalid path spec: " + path, e);
                        }
                    }
                }

                dotAllowed = true;
                fieldRequired = false;

                continue;
            }
            else {
                if (token.charAt(0) == '?') {
                    if ("?entrySet".equals(token)) {
                        result.add(new MapEntrySetStep());
                    }
                    else {
                        throw new IllegalArgumentException("Invalid path spec: " + path);
                    }
                }
                else if (dotAllowed && token.equals("*")) {
                    if (lastIsDoubleAsterisk(result)) {
                        throw new IllegalArgumentException("Invalid path spec: " + path);
                    }
                    else if (lastIsAsterisk(result)) {
                        if (strictPath) {
                            throw new IllegalArgumentException("Invalid path spec: " + path);
                        }
                        result.set(result.size() - 1, new AnyPathStep());
                    }
                    else {
                        result.add(new FieldStep(null));
                    }
                }
                else {
                    result.add(new FieldStep(token.equals("*") ? null : token));
                }

                dotAllowed = true;
                fieldRequired = false;
                continue;
            }
        }

        return result.toArray(new PathStep[result.size()]);
    }

    private static boolean lastIsAsterisk(List<PathStep> result) {
        if (result.isEmpty()) {
            return false;
        }
        else {
            PathStep step = result.get(result.size() - 1);
            if (step instanceof FieldStep && ((FieldStep)step).getFieldName() == null) {
                return true;
            }
        }
        return false;
    }

    private static boolean lastIsDoubleAsterisk(List<PathStep> result) {
        if (result.isEmpty()) {
            return false;
        }
        else {
            PathStep step = result.get(result.size() - 1);
            if (step instanceof AnyPathStep) {
                return true;
            }
        }
        return false;
    }

    private static String token(int n, String path) {
        StringBuilder sb = new StringBuilder();
        while(n < path.length()) {
            char ch = path.charAt(n);
            if (ch == '.') {
                if (sb.length() == 0) {
                    return ".";
                }
                else {
                    return sb.toString();
                }
            }
            if (ch == '*') {
                if (sb.length() == 0) {
                    return "*";
                }
                else {
                    throw new IllegalArgumentException("Invalid path spec: " + path);
                }
            }
            if (ch == '(') {
                if (sb.length() == 0) {
                    return group(n, path);
                }
                else {
                    return sb.toString();
                }
            }
            if (ch == '[') {
                if (sb.length() == 0) {
                    return group(n, path);
                }
                else {
                    return sb.toString();
                }
            }
            if (ch == '?') {
                if (sb.length() == 0) {
                    sb.append(ch);
                }
                else {
                    return sb.toString();
                }
            }
            else if (Character.isJavaIdentifierStart(ch)) {
                sb.append(ch);
            }
            else if (Character.isJavaIdentifierPart(ch)) {
                if (sb.length() == 0) {
                    throw new IllegalArgumentException("Invalid path spec: " + path);
                }
                sb.append(ch);
            }
            else {
                throw new IllegalArgumentException("Invalid path spec: " + path);
            }
            ++n;
        }
        return sb.toString();
    }

    private static String group(int n, String path) {
        char ch = path.charAt(n);
        if (ch == '[') {
            ch = ']';
        }
        else if (ch == '(') {
            ch = ')';
        }
        else {
            throw new IllegalArgumentException("Invalid path spec: " + path);
        }

        int m = n;
        ++n;
        while(n < path.length()) {
            if (path.charAt(n) == ch) {
                return path.substring(m, n + 1);
            }
            ++n;
        }
        throw new IllegalArgumentException("Invalid path spec: " + path);
    }

    static Set<Instance> collect(Instance instance, PathStep[] steps) {

        Set<Instance> active = new HashSet<Instance>();
        Set<Instance> next = new HashSet<Instance>();
        active.add(instance);

        for(PathStep step: steps) {
            for(Instance i: active) {
                Iterator<Instance> it = step.walk(i);
                while(it.hasNext()) {
                    Instance sub = it.next();
                    if (sub != null) {
                        next.add(sub);
                    }
                }
            }
            // swap buffers
            active.clear();
            Set<Instance> s = active;
            active = next;
            next = s;
            if (active.isEmpty()) {
                return active;
            }
        }

        return active;

    }

}
