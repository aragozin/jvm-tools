package org.gridkit.jvmtool.heapdump;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HeapPathParserTest {

    @Parameters//(name = "\"{0}\" {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> result = new ArrayList<Object[]>();

        addCase(result, "inputsByName.table[*].value", true, "[inputsByName, table, [*], value]");
        addCase(result, "inputsByName.table[*].value(**.String)", true, "[inputsByName, table, [*], value, (**.String)]");
        addCase(result, "inputsByName.table?entrySet.value(**.String)", true, "[inputsByName, table, ?entrySet, value, (**.String)]");
        addCase(result, "inputsByName.*.table[*].value(**.String)", true, "[inputsByName, *, table, [*], value, (**.String)]");
        addCase(result, "inputsByName.**.table[*].value(**.String)", true, null);
        addCase(result, "inputsByName.**.table[*].value(**.String)", true, null);
        addCase(result, "inputsByName.**.table[*].value(**.String)", false, "[inputsByName, **, table, [*], value, (**.String)]");
        addCase(result, "inputsByName.***.table[*].value(**.String)", true, null);
        addCase(result, "inputsByName.***.table[*].value(**.String)", false, null);
        addCase(result, "inputsByName.*.*.table[*].value(**.String)", true, "[inputsByName, *, *, table, [*], value, (**.String)]");
        addCase(result, "inputsByName.**.*.table[*].value(**.String)", false, "[inputsByName, **, *, table, [*], value, (**.String)]");
        addCase(result, "inputsByName.table?entrySet[key=123].value(**.String)", true, "[inputsByName, table, ?entrySet, [key=123], value, (**.String)]");
        addCase(result, "(**.String)", true, "[(**.String)]");
        addCase(result, "(**.String)", false, "[(**.String)]");

        return result;
    }

    private static void addCase(List<Object[]> list, Object... args) {
        list.add(args);
    }

    String expr;
    boolean strictPath;
    String expected;

    public HeapPathParserTest(String expr, boolean strictPath, String expected) {
        this.expr = expr;
        this.strictPath = strictPath;
        this.expected = expected;
    }

    @Test
    public void testExpr() {

        System.out.println("EXPR: " + expr);

        if (expected != null) {
            String text;
            text = Arrays.toString(HeapPath.parsePath(expr, strictPath));
            System.out.println(text);
            assertThat(text).isEqualTo(expected);
        }
        else {
            try {
                System.out.println(Arrays.toString(HeapPath.parsePath(expr, strictPath)));
                Assert.fail("Exception expected");
            }
            catch(IllegalArgumentException e) {
                // expected
            }
        }
    }
}
