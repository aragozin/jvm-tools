package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassificatorAST {

    public Root newRoot() {
        return new Root();
    }
    
    public static Filter disjunction(Collection<Filter> filters) {
        if (filters.isEmpty()) {
            return new FalseFilter();
        }
        else if (filters.size() == 1) {
            return filters.iterator().next();
        }
        else {
            return new AndCombinatorFilter(filters);
        }
    }

    public static Filter conjunction(Collection<Filter> filters) {
        if (filters.isEmpty()) {
            return new FalseFilter();
        }
        else if (filters.size() == 1) {
            return filters.iterator().next();
        }
        else {
            return new OrCombinatorFilter(filters);
        }
    }
    
    public static class Root {
        
        public Map<String, Classification> classifications = new LinkedHashMap<String, Classification>();
    }
    
    public static class Classification {
        
        public String name;
        
        public Map<String, Filter> subclasses = new LinkedHashMap<String, Filter>();
        public Filter rootFilter;
        
    }
    
    public static interface Filter {
        
    }

    public static interface FrameMatcher {
        
    }

    public static class AnyOfFrameMatcher implements FrameMatcher {
        
        public List<FrameMatcher> submatchers = new ArrayList<FrameMatcher>();
        
        public AnyOfFrameMatcher() {            
        }

        public AnyOfFrameMatcher(List<FrameMatcher> submatchers) {
            this.submatchers.addAll(submatchers);
        }
    }
    
    public static class OrCombinatorFilter implements Filter {
        
        public List<Filter> subfilters = new ArrayList<Filter>();
        
        public OrCombinatorFilter() {            
        }

        public OrCombinatorFilter(Collection<Filter> subfilters) {
            subfilters.addAll(subfilters);
        }
    }

    public static class AndCombinatorFilter implements Filter {
        
        public List<Filter> subfilters = new ArrayList<Filter>();

        public AndCombinatorFilter() {            
        }

        public AndCombinatorFilter(Collection<Filter> subfilters) {
            subfilters.addAll(subfilters);
        }
    }

    public static class PatternFilter implements Filter, FrameMatcher {
        
        public List<String> patterns = new ArrayList<String>();
    }

    public static class TrueFilter implements Filter, FrameMatcher {
        
    }

    public static class FalseFilter implements Filter, FrameMatcher {
        
    }
    
    public static class NotFilter {
        
        public Filter nested;
        
    }

    public static class LastFollowedFilter implements Filter {
        
        public FrameMatcher snippet;
        public Filter followFilter;
    }

    public static class LastNotFollowedFilter implements Filter {
        
        public FrameMatcher snippet;
        public Filter followFilter;
    }
}
