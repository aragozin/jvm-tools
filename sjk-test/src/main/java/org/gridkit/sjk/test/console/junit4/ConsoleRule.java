package org.gridkit.sjk.test.console.junit4;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Supplier;
import org.gridkit.sjk.test.console.ConsoleTracker;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class ConsoleRule extends TestWatcher {

    public static ConsoleRule out() {
        return new ConsoleRule(ConsoleTracker.out());
    }

    public static ConsoleRule err() {
        return new ConsoleRule(ConsoleTracker.err());
    }

    private final ConsoleTracker tracker;
    private final List<Runnable> postInitHooks = new ArrayList<Runnable>();

    public ConsoleRule(ConsoleTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    protected void starting(Description description) {
        super.starting(description);
        tracker.init();
        for (Runnable r: postInitHooks) {
            r.run();
        }
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        tracker.complete();
    }


    /**
     * Some loggers may need reconfiguration to start logging to overriden our/err streams.
     */
    public ConsoleRule postInit(Runnable r) {
        this.postInitHooks.add(r);
        return this;
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

    public void clean() {
        tracker.clean();
    }

    public ConsoleRule skip() {
        tracker.skip();
        return this;
    }

    public ConsoleRule skip(int lines) {
        tracker.skip(lines);
        return this;
    }

    public ConsoleRule line(String exact) {
        tracker.line(exact);
        return this;
    }

    public ConsoleRule lineStarts(String starts) {
        tracker.lineStarts(starts);
        return this;
    }

    public ConsoleRule lineStartsEx(String starts, String... vars) {
        tracker.lineStartsEx(starts, vars);
        return this;
    }

    public ConsoleRule lineContains(String... substring) {
        tracker.lineContains(substring);
        return this;
    }

    public ConsoleRule lineContainsEx(String substring, String... vars) {
        tracker.lineContainsEx(substring, vars);
        return this;
    }

    public ConsoleRule lineEx(String pattern, String... vars) {
        tracker.lineEx(pattern, vars);
        return this;
    }

    @Override
    public String toString() {
        return tracker.toString();
    }
}
