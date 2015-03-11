package org.gridkit.jvmtool.stacktrace.analytics;

import static org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.conjunction;
import static org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.disjunction;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.AnyOfFrameMatcher;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Classification;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Filter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.FrameMatcher;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.LastFollowedFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.LastNotFollowedFilter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Root;
import org.gridkit.jvmtool.stacktrace.util.IndentParser;

public class ClassificatorParser extends IndentParser {

    private static boolean diag = false;
    
    private static final String CAT_NAME = "\\[(.*)\\]";
    private static final String SUBCLASS_NAME = "\\+(.*)";
    private static final String FRAME_PATTERN = "([^!]+)";
    
    private Matcher tokenMatcher;
    private Root result;
    
    public Root getResult() {
        return result;
    }
    
    @Override
    protected void initialState() {
        RootSS root = new RootSS();
        this.result = root.root;
        pushState(root);
    }

    private void pushState(SyntaticState ss) {
        if (!(ss instanceof RootSS)) {
            pushParseState();
        }
        ss.initState();
        pushValue(ss);
        if (diag) {
            System.err.println(">> " + ss.getClass().getSimpleName());
        }
    }

    protected void replaceState(SyntaticState ss) {
        Object prev = popValue();
        unexpectAll();
        if (diag) {
            System.err.println("<< " + prev.getClass().getSimpleName() + " >> " + ss.getClass().getSimpleName());
        }
        ss.initState();
        pushValue(ss);        
    }

    @Override
    protected void onToken(String tokenType, String token) throws ParseException {
        dispatchTokenByValue(tokenType, token);
    }

    private void matchToken(String tokenType, String token, String pattern) {
        Pattern ptr = Pattern.compile(pattern);
        tokenMatcher = ptr.matcher(token);
        if (!tokenMatcher.matches()) {
            throw new IllegalArgumentException("Token doesn't match pattern: " + token);
        }
    }

    private void dispatchTokenByValue(String tokenType, String token) throws ParseException {
        try {
            SyntaticState state = (SyntaticState) value();
            String name = "onToken" + tokenType;
            Method m = state.getClass().getMethod(name);
            String pattern = m.getAnnotation(Token.class).value();
            matchToken(tokenType, token, pattern);
            m.setAccessible(true);
            m.invoke(state);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ParseException) {
                throw ((ParseException)e.getCause());
            }
            else {
                throw new RuntimeException(e.getTargetException());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onPopup() throws ParseException {
        try {
            SyntaticState state = (SyntaticState) value();
            popValue();
            popParseState();
            if (diag) {
                System.err.println("<< " + state.getClass().getSimpleName());
            }
            String name = "onPopup";
            Method m = state.getClass().getMethod(name);
            m.setAccessible(true);
            m.invoke(state);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ParseException) {
                throw ((ParseException)e.getCause());
            }
            else {
                throw new RuntimeException(e.getTargetException());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    abstract class SyntaticState {
        
        public void initState() {
            for(Method m: getClass().getMethods()) {
                if (m.getName().startsWith("onToken")) {
                    String tn = m.getName().substring("onToken".length());
                    String pattern = m.getAnnotation(Token.class).value();
                    expectToken(tn, pattern);
                }
                else if (m.getName().startsWith("onPopup")) {
                    expectPopup();
                }
            }
        }        

        public void initClassState(Class<?> c) {
            for(Method m: c.getDeclaredMethods()) {
                if (m.getName().startsWith("onToken")) {
                    String tn = m.getName().substring("onToken".length());
                    String pattern = m.getAnnotation(Token.class).value();
                    expectToken(tn, pattern);
                }
                else if (m.getName().startsWith("onPopup")) {
                    expectPopup();
                }
            }
        }       
        
        public void popupState() throws ParseException {
            ClassificatorParser.this.onPopup();
        }
    }
    
    class RootSS extends SyntaticState {

        Root root = new ClassificatorAST.Root();
     
        @Token(CAT_NAME)
        public void onTokenCategory() {
            String name = tokenMatcher.group(1);
            ClassificationSS cat = new ClassificationSS();
            cat.root = this;
            cat.classification.name = name;
            replaceState(cat);
            pushState(new RootFilterSS(cat));
        }
        
        public void onPopup() {
            result = root;
        }
    }

    class ClassificationSS extends SyntaticState {
        
        RootSS root;
        Classification classification = new ClassificatorAST.Classification();
        List<Filter> filters = new ArrayList<Filter>();
        int line;
        int pos;
        
        public ClassificationSS() {
            line = getLineNumber();
            pos = getIndent();
        }

        @Token(CAT_NAME)
        public void onTokenCategory() throws ParseException {
            onPopup();
            root.onTokenCategory();
        }

        @Token(SUBCLASS_NAME)
        public void onTokenSubclass() {
            SubclassSS ss = new SubclassSS(tokenMatcher.group(1), this);
            pushState(ss);
        }
        
        public void onPopup() throws ParseException {
            if (root.root.classifications.containsKey(classification.name)) {
                throw new ParseException("Name '" + classification.name + "' is already used", line, pos);
            }
            
            classification.rootFilter = conjunction(filters);
            root.root.classifications.put(classification.name, classification);
        }
    }

    class RootFilterSS extends FilterSS {
        
        ClassificationSS parent;

        public RootFilterSS(ClassificationSS parent) {
            super();
            this.parent = parent;
        }

        @Override
        protected void push(Filter filter) throws ParseException {
            parent.filters.add(filter);
        }
        
        public void onPopup() {
        }
    }
    
    abstract class FilterSS extends SyntaticState {
        
        @Token(FRAME_PATTERN)
        public void onTokenPattern() throws ParseException {
            String p = tokenMatcher.group(1);
            ClassificatorAST.PatternFilter f = new ClassificatorAST.PatternFilter();
            f.patterns.add(p);
            pushState(new PopupOnlySS());
            push(f);
        }
        
        @Token("!(REQUIRE\\s+)?ALL")
        public void onTokenAnd() {
            FilterSS ss = new AndCombinatorSS(this);
            pushState(ss);
        }

        @Token("!(REQUIRE\\s+)?ANY")
        public void onTokenAny() {
            FilterSS ss = new OrCombinatorSS(this);
            pushState(ss);
        }

        @Token("!LAST\\s+FRAME")
        public void onTokenLastFrame() {
            LastFrameSS ss = new LastFrameSS(this);
            pushState(ss);
        }
        
        protected abstract void push(ClassificatorAST.Filter filter) throws ParseException;
    }
    
    class AndCombinatorSS extends FilterSS {

        FilterSS parent;
        List<Filter> filters = new ArrayList<ClassificatorAST.Filter>();
        
        public AndCombinatorSS(FilterSS parent) {
            this.parent = parent;
        }

        @Override
        protected void push(Filter filter) throws ParseException {
            filters.add(filter);
        }
        
        public void onPopup() throws ParseException {
            if (filters.isEmpty()) {
                error("Empty !ALL group");
                return;
            }
            parent.push(disjunction(filters));            
        }
    }

    class OrCombinatorSS extends FilterSS {

        FilterSS parent;
        List<Filter> filters = new ArrayList<ClassificatorAST.Filter>();
        
        public OrCombinatorSS(FilterSS parent) {
            this.parent = parent;
        }

        @Override
        protected void push(Filter filter) throws ParseException {
            filters.add(filter);
        }
        
        public void onPopup() throws ParseException {
            if (filters.isEmpty()) {
                error("Empty !ANY group");
                return;
            }
            parent.push(conjunction(filters));            
        }
    }
    
    class PopupOnlySS extends SyntaticState {

        public void onPopup() throws ParseException {
        }
    }
    
    class SubclassSS extends FilterSS {

        ClassificationSS parent;
        String name;        
        List<Filter> filters = new ArrayList<Filter>();
        int line;
        int pos;
        
        public SubclassSS(String name, ClassificationSS parent) {
            this.name = name;
            this.parent = parent;
            line = getLineNumber();
            pos = getIndent();
        }

        @Token(CAT_NAME)
        public void onTokenCategory() throws ParseException {
            popupState();
            parent.onTokenCategory();
        }

        @Token(SUBCLASS_NAME)
        public void onTokenSubclass() throws ParseException {
            popupState();
            parent.onTokenSubclass();
        }
        
        @Override
        protected void push(Filter filter) {
            this.filters.add(filter);
        }
        
        public void onPopup() throws ParseException {
            Classification cls = parent.classification;
            if (cls.subclasses.containsKey(name)) {
                error("Subclass '" + name + "' is already defined for [" + cls.name + "]");
            }
            else {
                cls.subclasses.put(name, ClassificatorAST.conjunction(filters));
            }
        }
    }

    interface PositionedPredicate {
        
        public void finish(Filter filter) throws ParseException;
        
    }
    
    class LastFrameSS extends SyntaticState implements PositionedPredicate {
        
        FilterSS parent;
        List<FrameMatcher> frames = new ArrayList<FrameMatcher>();
        boolean followed;
        boolean done;
        
        public LastFrameSS(FilterSS parent) {
            this.parent = parent;
        }
                
        @Override
        public void initState() {
            super.initState();
        }

        @Token(FRAME_PATTERN)
        public void onTokenPattern() throws ParseException {
            String p = tokenMatcher.group(1);
            ClassificatorAST.PatternFilter f = new ClassificatorAST.PatternFilter();
            f.patterns.add(p);
            pushState(new PopupOnlySS());
            frames.add(f);
        }

        @Token("!FOLLOWED")
        public void onTokenFollowed() throws ParseException {
            followed = true;
            if (frames.isEmpty()) {
                error("No frames to match");
            }
            PositionedFilterSS ss = new PositionedFilterSS(this);
            replaceState(ss);
        }

        @Token("!NOT\\s+FOLLOWED")
        public void onTokenNotFollowed() throws ParseException {
            followed = false;
            if (frames.isEmpty()) {
                error("No frames to match");
            }
            PositionedFilterSS ss = new PositionedFilterSS(this);
            replaceState(ss);
        }
        
        @Override
        public void finish(Filter filter) throws ParseException {
            done = true;
            if (followed) {
                LastFollowedFilter lff = new LastFollowedFilter();
                if (frames.size() == 1) {
                    lff.snippet = frames.get(0);
                }
                else {
                    lff.snippet = new AnyOfFrameMatcher(frames);
                }
                lff.followFilter = filter;
                parent.push(lff);
            }            
            else {
                LastNotFollowedFilter lff = new LastNotFollowedFilter();
                if (frames.size() == 1) {
                    lff.snippet = frames.get(0);
                }
                else {
                    lff.snippet = new AnyOfFrameMatcher(frames);
                }
                lff.followFilter = filter;                
                parent.push(lff);
            }            
        }
    }
    
    class PositionedFilterSS extends FilterSS {

        PositionedPredicate pred;
        List<Filter> filters = new ArrayList<ClassificatorAST.Filter>();
        
        
        public PositionedFilterSS(PositionedPredicate pred) {
            this.pred = pred;
        }

        @Override
        protected void push(Filter filter) throws ParseException {
            filters.add(filter);
        }
        
        public void onPopup() throws ParseException {
            if (filters.isEmpty()) {
                error("Empty condition");
                return;
            }
            pred.finish(disjunction(filters));
        }
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface Token {
        String value();
    }
}
