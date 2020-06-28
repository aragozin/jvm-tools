package org.gridkit.jvmtool.stacktrace.analytics;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.gridkit.jvmtool.stacktrace.ReaderProxy;
import org.gridkit.jvmtool.stacktrace.StackTraceCodec;
import org.gridkit.jvmtool.stacktrace.StackTraceReader;
import org.gridkit.jvmtool.stacktrace.ThreadSnapshot;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FilterParserCorpusTest {

    private static List<Object[]> cases = new ArrayList<Object[]>();

    @Parameters(name = "\"{0}\" {1}")
    public static List<Object[]> getExpressions() {
        addCase(10912, "**");
        addCase(10912, "org.apache.catalina.connector.CoyoteAdapter.service");
        addCase(1467,  "**.CoyoteAdapter.service+org.jboss.jca.adapters.jdbc,javax.jdbc");
        addCase(2891,  "**.CoyoteAdapter.service+org.hibernate.internal.SessionImpl.autoFlushIfRequired");
        addCase(2605,  "**.CoyoteAdapter.service+org.hibernate.internal.SessionImpl.autoFlushIfRequired!org.jboss.jca.adapters.jdbc,javax.jdbc");
        addCase(439,   "**.CoyoteAdapter.service+org.hibernate.!org.hibernate.internal.SessionImpl.autoFlushIfRequired!org.jboss.jca.adapters.jdbc,javax.jdbc");
        addCase(1544,  "**.CoyoteAdapter.service+com.sun.faces.facelets.compiler.Compiler.compile");
        addCase(1287,  "**.CoyoteAdapter.service+javax.xml.");
        addCase(38,    "**.CoyoteAdapter.service+javax.xml.!com.sun.faces.facelets.compiler.Compiler.compile");
        addCase(256,   "**.CoyoteAdapter.service+java.util.ResourceBundle.getObject*ResourceBundle.java:395");
        addCase(256,   "**.CoyoteAdapter.service+java.util.ResourceBundle.getObject*:395");
        addCase(933,   "**.CoyoteAdapter.service+java.util.ResourceBundle.getObject");
        addCase(677,   "**.CoyoteAdapter.service+java.util.ResourceBundle.getObject!java.util.ResourceBundle.getObject*:395");
        addCase(3584,  "**.CoyoteAdapter.service+org.jboss.seam.core.BijectionInterceptor.aroundInvoke,org.jboss.seam.core.SynchronizationInterceptor.aroundInvoke+**.proceed");
        addCase(0,     "**.CoyoteAdapter.service+org.jboss.seam.core.BijectionInterceptor.aroundInvoke,org.jboss.seam.core.SynchronizationInterceptor.aroundInvoke!**.proceed");
        addCase(1362,  "**.CoyoteAdapter.service+org.jboss.seam.core.BijectionInterceptor.aroundInvoke,org.jboss.seam.core.SynchronizationInterceptor.aroundInvoke/!**.proceed");
        addCase(934,   "**.CoyoteAdapter.service+org.jboss.seam.core.BijectionInterceptor.aroundInvoke,org.jboss.seam.core.SynchronizationInterceptor.aroundInvoke/!**.proceed+org.jboss.seam.Component.*ject");
        addCase(428,   "**.CoyoteAdapter.service+org.jboss.seam.core.BijectionInterceptor.aroundInvoke,org.jboss.seam.core.SynchronizationInterceptor.aroundInvoke/!**.proceed+java.util.concurrent.locks.ReentrantLock");
        addCase(2891,   "org.hibernate/^+**.onAutoFlush");
        addCase(1542,   "org.hibernate/^!**.onAutoFlush");

        return cases;
    }

    private static void addCase(int matchCount, String filter) {
        cases.add(new Object[]{filter, matchCount});
    }

    private String filter;
    private int matchCount;

    public FilterParserCorpusTest(String filter, int matchCount) {
        this.filter = filter;
        this.matchCount = matchCount;
    }

    @Test
    public void verify() throws FileNotFoundException, IOException {
        ThreadSnapshotFilter f = TraceFilterPredicateParser.parseFilter(filter, new CachingFilterFactory());
        StackTraceReader reader = StackTraceCodec.newReader(new FileInputStream("src/test/resources/jboss-10k.std"));
        int n = 0;
        if (!reader.isLoaded()) {
            reader.loadNext();
        }
        ThreadSnapshot readerProxy = new ReaderProxy(reader);
        while(reader.isLoaded()) {
            if (f.evaluate(readerProxy)) {
                ++n;
            }
            if (!reader.loadNext()) {
                break;
            }
        }
        Assert.assertEquals(matchCount, n);
    }

}
