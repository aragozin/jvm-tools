package org.gridkit.jvmtool;

import org.junit.Assert;
import org.junit.Test;


public class RegExHelperTest {

    @Test
    public void verify_simple() {
        Assert.assertEquals("(?:.*)", RegExHelper.uncapture("(.*)"));
    }

    @Test
    public void verify_escape() {
        Assert.assertEquals("(?:\\(.*\\))", RegExHelper.uncapture("(\\(.*\\))"));
    }

    @Test
    public void verify_charclass() {
        Assert.assertEquals("(?:[(].*[)])", RegExHelper.uncapture("([(].*[)])"));
    }

    @Test
    public void verify_quote() {
        Assert.assertEquals("(?:\\Q()\\E)", RegExHelper.uncapture("(\\Q()\\E)"));
    }
}
