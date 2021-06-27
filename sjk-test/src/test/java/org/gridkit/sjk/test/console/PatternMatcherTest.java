package org.gridkit.sjk.test.console;

import org.gridkit.sjk.test.console.PatternMatcher.PatternNode;
import org.junit.Assert;
import org.junit.Test;

public class PatternMatcherTest {

    private PatternMatcher.PatternNode seq(PatternNode... mm) {
        return new PatternMatcher.SequenceNode(mm);
    }

    private PatternMatcher.PatternNode alt(PatternNode... mm) {
        return new PatternMatcher.AlternativesNode(mm);
    }

    private PatternMatcher.PatternNode any() {
        return new PatternMatcher.AnyLinesNode();
    }

    private PatternMatcher.PatternNode lit(String text) {
        return new PatternMatcher.MatchExact(text);
    }

    @Test
    public void test_simple_matches() {

        String text =
                "a1\n" +
                "a2\n" +
                "a3\n" +
                "a4";


        Assert.assertTrue(new PatternMatcher(
                seq(lit("a1"), any())
                ).matchWhole(text));

        Assert.assertFalse(new PatternMatcher(
                seq(lit("a2"), any())
                ).matchWhole(text));

        Assert.assertTrue(new PatternMatcher(
                seq(any(), lit("a4"))
                ).matchWhole(text));

        Assert.assertTrue(new PatternMatcher(
                seq(any(), lit("a4"), any())
                ).matchWhole(text));

        Assert.assertFalse(new PatternMatcher(
                seq(any(), lit("a3"))
                ).matchWhole(text));

        Assert.assertTrue(new PatternMatcher(
                seq(any(), lit("a3"), any())
                ).matchWhole(text));

        Assert.assertTrue(new PatternMatcher(
                seq(alt(lit("a1"), lit("a2")), any())
                ).matchWhole(text));

        Assert.assertTrue(new PatternMatcher(
                seq(alt(lit("a2"), lit("a1")), any())
                ).matchWhole(text));
    }
}
