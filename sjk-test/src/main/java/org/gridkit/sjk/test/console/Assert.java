package org.gridkit.sjk.test.console;

/**
 * Assertion utility to avoid runtime dependency of specific JUnit version.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class Assert {

    public static void fail(String message) {
        throw new AssertionError(message);
    }
}
