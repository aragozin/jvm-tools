package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Classification;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Filter;
import org.gridkit.jvmtool.stacktrace.analytics.ClassificatorAST.Root;

public class ClassificatorParser extends AbstractClassificatorParser {

    public Root getResult() {
        return ((RootW)getRoot()).root;
    }

    @Override
    protected ARoot newRootNode() {
        Root root = new Root();
        return new RootW(root);
    }

    @Override
    protected AClassification newClassificationNode(String name) {
        Classification c = new Classification();
        c.name = name;
        return new ClassificationW(c);
    }

    @Override
    protected void addToRoot(ARoot root, AClassification classification) {
        RootW rw = (RootW) root;
        ClassificationW cw = (ClassificationW) classification;
        
        if (rw.root.classifications.containsKey(cw.classification.name)) {
            throw new IllegalArgumentException("Classification '" + cw.classification.name + "' is already defined");
        }
        
        rw.root.classifications.put(cw.classification.name, cw.classification);
    }

    @Override
    protected void addSubclass(AClassification classification, String name, Filter filter) {
        ClassificationW cw = (ClassificationW) classification;

        if (cw.classification.subclasses.containsKey(name)) {
            throw new IllegalArgumentException("Subclass '" + name + "' is already defined");
        }

        cw.classification.subclasses.put(name, filter);        
    }

    class RootW implements ARoot {
        
        Root root;

        public RootW(Root root) {
            this.root = root;
        }
    }
    
    class ClassificationW implements AClassification {
        
        Classification classification;

        public ClassificationW(Classification c) {
            this.classification = c;
        }

        @Override
        public void setRootFilter(Filter conjunction) {
            classification.rootFilter = conjunction;
        }
    }
}
