package org.gridkit.sjk.test.console.junit5;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ConsoleExtensionTest {

    @RegisterExtension
    public ConsoleExtension console = ConsoleExtension.out();

    @Test
    public void console_test_positive() {
        System.out.println("Match me");
        console.line("Match me");
    }

    @Test
    public void console_test_multi_line_positive() {
        System.out.println("You cannot");
        System.out.println("Match me");
        System.out.println("Ever");
        console
            .skip()
            .line("Match me");
    }

    @Test
    public void console_test_negative() {
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
}
