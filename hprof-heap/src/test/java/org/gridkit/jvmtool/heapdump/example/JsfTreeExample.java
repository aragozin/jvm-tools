package org.gridkit.jvmtool.heapdump.example;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gridkit.jvmtool.heapdump.HeapHistogram;
import org.gridkit.jvmtool.heapdump.HeapWalker;
import org.gridkit.jvmtool.util.TextTree;
import org.junit.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

/**
 * This example demonstrates extracting JSF component trees 
 * from heap dump.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JsfTreeExample {

    /**
     * This entry point for this example.
     */
    @Test
    public void check() throws FileNotFoundException, IOException {
        String dumppath = ""; // path to dump of JEE server
        Heap heap = HeapFactory.createFastHeap(new File(dumppath));
        dumpComponentTree(heap);
    }
    
    /**
     * Print JSF component tree extracted from heap dump.
     */
    public void dumpComponentTree(Heap heap) {
        
        Set<JavaClass> compClasses = new HashSet<JavaClass>();
        Set<Instance> roots = new HashSet<Instance>();
        Map<Instance, List<Instance>> links = new HashMap<Instance, List<Instance>>();

        // Collect all subclasses of javax.faces.component.UIComponent 
        for(JavaClass jc: heap.getAllClasses()) {
           if (isComponent(jc)) {
               compClasses.add(jc);
           }
        }
        
        System.out.println("UIComponent classes: " + compClasses.size());

        int total = 0;
        
        // Scan whole heap in search for UIComponent instances
        for(Instance i: heap.getAllInstances()) {
            if (!compClasses.contains(i.getJavaClass())) {
                continue;
            }
            ++total;
            
            // For each node find root and retain it in roots collection
            Instance v = HeapWalker.valueOf(i, "compositeParent");
            v = v != null ? v : HeapWalker.<Instance>valueOf(i, "parent");
            if (v == null) {
                roots.add(i);
            }
            else {
                // collect parent-to-child relations
                // as they are hard to extract 
                // from parent component instance
                if (!links.containsKey(v)) {
                    links.put(v, new ArrayList<Instance>());
                }
                links.get(v).add(i);
            }
        }
        
        System.out.println("Found " + roots.size() + " component tree roots and " + total + " nodes in total");
        
        // Report tree for each root UIComponent found before
        for(Instance root: roots) {
            HeapHistogram hh = new HeapHistogram();
            // links variable contains all edges in component graphs identified during heap scan
            collect(hh, root, links);
            System.out.println();
            System.out.println(root.getInstanceId());
            System.out.println(hh.formatTop(10));
            System.out.println();
            // Dump may contain partial trees
            // Report only reasonably large object clusters
            if (hh.getTotalCount() > 500) {
                printTree(root, links);
                break;
            }
        }
    }
    
    private void printTree(Instance root, Map<Instance, List<Instance>> links) {
        TextTree tree = tree(root, links);
        System.out.println(tree.printAsTree());
    }

    // TextTree is a helper class to output ASCII formated tree
    private TextTree tree(Instance node, Map<Instance, List<Instance>> links) {
        List<TextTree> c = new ArrayList<TextTree>();
        List<Instance> cc = links.get(node);
        if (cc != null) {
            for(Instance i: cc) {
                c.add(tree(i, links));
            }
        }
        return display(node, c.toArray(new TextTree[0]));
    }
    
    private TextTree display(Instance node, TextTree[] children) {
        String nodeType = simpleName(node.getJavaClass().getName());
        String info = "id:" + HeapWalker.valueOf(node, "id");
        String el = HeapWalker.valueOf(node, "txt.literal");
        if (el != null) {
            info += " el:" + el.replace('\n', ' ');
        }
        TextTree c = TextTree.t("#", children);
        
        return children.length == 0 
                    ? TextTree.t(nodeType, TextTree.t(info))
                    : TextTree.t(nodeType, TextTree.t(info), c);
    }

    private void collect(HeapHistogram h, Instance node, Map<Instance, List<Instance>> links) {
        h.feed(node);
        List<Instance> cc = links.get(node);
        if (cc != null) {
            for(Instance i: cc) {
                collect(h, i, links);
            }
        }
    }

    private String simpleName(String name) {
        int c = name.lastIndexOf('.');
        return c < 0 ? name : name.substring(c + 1);
    }

    public boolean isComponent(JavaClass type) {
        if (type.getName().equals("javax.faces.component.UIComponent")) {
            return true;
        }
        else if (type.getSuperClass() != null) {
            return isComponent(type.getSuperClass());
        }
        else {
            return false;
        }
    }    
}
