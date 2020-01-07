package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public class SimpleCategorizer implements ThreadSnapshotCategorizer {

    private String[] categories = new String[0];
    private ThreadSnapshotFilter[] filters = new ThreadSnapshotFilter[0];

    public SimpleCategorizer() {
    }

    public SimpleCategorizer(Map<String, ThreadSnapshotFilter> categories) {
        for(Map.Entry<String, ThreadSnapshotFilter> cat: categories.entrySet()) {
            addCategory(cat.getKey(), cat.getValue());
        }
    }

    public void addCategory(String category, ThreadSnapshotFilter filter) {
        for(String cat: categories) {
            if (cat.equals(category)) {
                throw new IllegalArgumentException("Dupplicate category [" + cat + "]");
            }
        }
        int n = categories.length;
        categories = Arrays.copyOf(categories, n + 1);
        filters = Arrays.copyOf(filters, n + 1);
        categories[n] = category;
        filters[n] = filter;
    }

    @Override
    public List<String> getCategories() {
        return Arrays.asList(categories);
    }

    @Override
    public String categorize(ThreadSnapshot snap) {
        for(int i = 0; i != categories.length; ++i) {
            if (filters[i].evaluate(snap)) {
                return categories[i];
            }
        }
        return null;
    }
}
