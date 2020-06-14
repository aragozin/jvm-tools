package org.gridkit.jvmtool;

import java.util.concurrent.TimeUnit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CliCheckRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        CheckDuration duration = description.getAnnotation(CheckDuration.class);

        if (duration == null) {
            return base;
        }
        return new Autoshutdown(base, TimeUnit.SECONDS.toMillis(duration.value()));
    };

    static class Autoshutdown extends Statement {

        private final Statement fOriginalStatement;

        private final long fTimeout;

        public Autoshutdown(Statement originalStatement, long timeout) {
            fOriginalStatement = originalStatement;
            fTimeout = timeout;
        }

        @Override
        public void evaluate() throws Throwable {
            StatementThread thread = evaluateStatement();
            if (!thread.fFinished) {
                throwExceptionForUnfinishedThread(thread);
            }
        }

        private StatementThread evaluateStatement() throws InterruptedException {
            ThreadGroup tg = new ThreadGroup("TestGroup");
            StatementThread thread = new StatementThread(tg, fOriginalStatement);
            thread.start();
            thread.join(fTimeout);
            if (thread.isAlive()) {
                System.err.println("Shutdown command");
                thread.silentShutdown = true;
                shutdown(tg);
                thread.join(1000);
                shutdown(tg);
            }
            return thread;
        }

        private void shutdown(ThreadGroup tg) {
            int tc = tg.enumerate(new Thread[0], true);
            Thread[] threads = new Thread[2 * tc];
            tg.enumerate(threads, true);
            for (Thread th: threads) {
                if (th != null) {
                    th.interrupt();
                }
            }

        }

        private void throwExceptionForUnfinishedThread(StatementThread thread)
                throws Throwable {
            if (thread.fExceptionThrownByOriginalStatement != null) {
                throw thread.fExceptionThrownByOriginalStatement;
            }
        }

        private static class StatementThread extends Thread {
            private final Statement fStatement;

            private boolean fFinished = false;

            private Throwable fExceptionThrownByOriginalStatement = null;

            private boolean silentShutdown;

            public StatementThread(ThreadGroup group, Statement statement) {
                super(group, "TestThread");
                fStatement = statement;
            }

            @Override
            public void run() {
                try {
                    fStatement.evaluate();
                    fFinished = true;
                } catch (InterruptedException e) {
                    // don't log the InterruptedException
                } catch (Throwable e) {
                    if (!silentShutdown) {
                        fExceptionThrownByOriginalStatement = e;
                    }
                }
            }
        }
    }
}
