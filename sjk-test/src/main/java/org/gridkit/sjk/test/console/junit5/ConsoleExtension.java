package org.gridkit.sjk.test.console.junit5;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Supplier;
import org.gridkit.sjk.test.console.ConsoleTracker;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ConsoleExtension implements BeforeEachCallback, AfterEachCallback {

    public static ConsoleExtension out() {
        return new ConsoleExtension(ConsoleTracker.out());
    }

    public static ConsoleExtension err() {
        return new ConsoleExtension(ConsoleTracker.err());
    }

    private final ConsoleTracker tracker;
    private final List<Runnable> postInitHooks = new ArrayList<Runnable>();

    ConsoleExtension(ConsoleTracker consoleTracker) {
        this.tracker = consoleTracker;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        tracker.init();
        for (Runnable r: postInitHooks) {
            r.run();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        tracker.complete();
        tracker.clean();
    }

    public void verify() {
        tracker.verify();
    }

    public boolean tryMatch() {
        return tracker.tryMatch();
    }

    public void waitForMatch(Supplier<Boolean> until) throws InterruptedException {
        tracker.waitForMatch(until);
    }

    public void complete() {
        tracker.complete();
    }

    public void clean() {
        tracker.clean();
    }

    public ConsoleExtension skipMax(int lines) {
        tracker.skipMax(lines);
        return this;
    }

    /**
     * Some loggers may need reconfiguration to start logging to overriden our/err streams.
     */
    public ConsoleExtension postInit(Runnable r) {
        this.postInitHooks.add(r);
        return this;
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
