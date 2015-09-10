package org.netbeans.lib.profiler.heap;

public class DummyD {

    public Sub nested;
    public Sub[] nestedArray;
    
    public DummyD() {        
    }    
    
    public static class Sub {
        
        String value;
        
    }
}
