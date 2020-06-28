package org.gridkit.jvmtool.stacktrace.analytics;

import java.util.List;

import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;

public interface ThreadSnapshotCategorizer {

    public List<String> getCategories();

    public String categorize(ThreadSnapshot snap);

}
