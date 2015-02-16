package org.gridkit.jvmtool.stacktrace;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class used for dictionary with expiring entries.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class RotatingStringDictionary {

    private Map<String, Integer> dic = new LinkedHashMap<String, Integer>();
    private int limit;

    public RotatingStringDictionary(int limit) {
        this.limit = limit;
    }

    public int intern(String str) {
        Integer id = dic.remove(str);
        if (id != null) {
            dic.put(str, id);
            return id;
        }
        else {
            if (dic.size() == limit) {
                Iterator<Integer> v = dic.values().iterator();
                id  = v.next();
                v.remove();
                dic.put(str, id);
                return ~id;
            }
            else {
                id = dic.size();
                dic.put(str, id);
                return ~id;
            }
        }
    }
}
