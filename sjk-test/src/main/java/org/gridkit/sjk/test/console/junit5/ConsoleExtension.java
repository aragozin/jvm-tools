package org.gridkit.sjk.test.console.junit5;

import org.gridkit.sjk.test.console.ConsoleTracker;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ConsoleExtension implements BeforeAllCallback, AfterAllCallback {

    public static ConsoleExtension out() {
        return new ConsoleExtension(ConsoleTracker.out());
    }

    public static ConsoleExtension err() {
        return new ConsoleExtension(ConsoleTracker.err());
    }

    private final ConsoleTracker tracker;

    ConsoleExtension(ConsoleTracker consoleTracker) {
        this.tracker = consoleTracker;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        tracker.init();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        tracker.complete();
    }

    public void verify() {
        tracker.verify();
    }

    public ConsoleExtension skip() {
        tracker.skip();
        return this;
    }

    public ConsoleExtension skip(int lines) {
        tracker.skip(lines);
        return this;
    }

    public ConsoleExtension line(String exact) {
        tracker.line(exact);
        return this;
    }

    public ConsoleExtension lineStarts(String starts) {
        tracker.lineStarts(starts);
        return this;
    }

    public ConsoleExtension lineStartsEx(String starts, String... vars) {
        tracker.lineStartsEx(starts, vars);
        return this;
    }

    public ConsoleExtension lineContains(String... substring) {
        tracker.lineContains(substring);
        return this;
    }

    public ConsoleExtension lineContainsEx(String substring, String... vars) {
        tracker.lineContainsEx(substring, vars);
        return this;
    }

    public ConsoleExtension lineEx(String pattern, String... vars) {
        tracker.lineEx(pattern, vars);
        return this;
    }

    @Override
    public String toString() {
        return tracker.toString();
    }
}
