package org.gridkit.jvmtool.heapdump;

import java.util.Iterator;

import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

public class HeapWalker {

    public static String stringValue(Instance obj) {
        if (obj == null) {
            return null;
        }
        if (!"java.lang.String".equals(obj.getJavaClass().getName())) {
            throw new IllegalArgumentException("Is not a string: " + obj.getInstanceId() + " (" + obj.getJavaClass().getName() + ")");
        }
        char[] text = null;
        int offset = 0;
        int len = -1;
        for(FieldValue f: obj.getFieldValues()) {
            Field ff = f.getField();
            if ("value".equals(ff.getName())) {
                PrimitiveArrayInstance chars = (PrimitiveArrayInstance) ((ObjectFieldValue)f).getInstance();
                text = new char[chars.getLength()];
                for(int i = 0; i != text.length; ++i) {
                    text[i] = ((String)chars.getValues().get(i)).charAt(0);
                }
            }
            // fields below were removed in Java 7
            else if ("offset".equals(ff.getName())) {
                offset = Integer.valueOf(f.getValue());
            }
            else if ("count".equals(ff.getName())) {
                len = Integer.valueOf(f.getValue());
            }
        }

        if (len < 0) {
            len = text.length;
        }

        return new String(text, offset, len);
    }

    public static Iterable<Instance> walk(Instance root, String path) {
        return HeapPathWalker.collect(root, HeapPathWalker.parsePath(path));
    }

    public static Instance walkFirst(Instance root, String path) {
        Iterator<Instance> it = walk(root, path).iterator();
        if (it.hasNext()) {
            return it.next();
        }
        else {
            return null;
        }
    }
}
