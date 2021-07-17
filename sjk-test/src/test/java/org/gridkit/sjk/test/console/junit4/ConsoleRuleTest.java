package org.gridkit.sjk.test.console.junit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ConsoleRuleTest {

    @Rule
    public ConsoleRule console = ConsoleRule.out();

    @Test
    public void console_test_simple_positive() {
        System.out.println("Match me");
        console.line("Match me");
    }

    @Test
    public void console_test_simple_negative() {
        System.out.println("Do not match me");
        console.line("Match me");
        try {
            console.verify();
        } catch (AssertionError e) {
            // expected
            return;
        }
        Assert.fail("Verification should fail");
    }

    @Test
    public void console_test_contains_positive1() {
        System.out.println("Term1 Term2 Term3");
        console.lineContains("Term3", "Term2", "Term1");
    }

    @Test
    public void console_test_contains_positive2() {
        System.out.println("--- Term1 Term2 Term3 --");
        console.lineContains("Term3", "Term2", "Term1");
    }

    @Test
    public void console_test_containes_negative() {
        System.out.println("Term1 Term2 Term4");
        console.lineContains("Term3", "Term2", "Term1");
        try {
            console.verify();
        } catch (AssertionError e) {
            // expected
            return;
        }
        Assert.fail("Verification should fail");
    }

    @Test
    public void console_test_containes_negative_first_line() {
        System.out.println("Term1 Term2 Term4");
        System.out.println("Term3 Term2");
        console.lineContains("Term3", "Term2", "Term1");
        console.lineContains("Term3", "Term2");
        try {
            console.verify();
        } catch (AssertionError e) {
            // expected
            return;
        }
        Assert.fail("Verification should fail");
    }

    @Test
    public void console_test_containes_negative_second_line() {
        System.out.println("Term1 Term2 Term4");
        System.out.println("Term3 Term2");
        console.lineContains("Term4", "Term2", "Term1");
        console.lineContains("Term1", "Term2");
        try {
            console.verify();
        } catch (AssertionError e) {
            // expected
            return;
        }
        Assert.fail("Verification should fail");
    }
}
