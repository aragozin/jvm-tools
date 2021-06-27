package org.gridkit.sjk.test.console.junit4;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.sjk.test.console.StopCommandAfter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit4 {@link Rule} for {@link CommandLauncher} based commands.
 * Command execution happens in process so debug is supported.
 * <p>
 * Supports {@link StopCommandAfter} annotation forcing command to be interrupted
 * after timeout. Which is useful for continuous monitoring commands.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CliTestRule implements TestRule {

    private final Class<?> mainClass;

    public final ConsoleRule out = ConsoleRule.out();
    public final ConsoleRule err = ConsoleRule.err();

    public CliTestRule() {
        this.mainClass = null;
    }

    public CliTestRule(Class<?> mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        StopCommandAfter duration = description.getAnnotation(StopCommandAfter.class);

        base = out.apply(base, description);
        base = err.apply(base, description);

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
                System.err.println("Command interrupted - @StopCommandAfter");
                thread.silentShutdown = true;
                shutdown(tg);
                thread.join(1000);
                shutdown(tg);
            }
            join(tg);
            return thread;
        }

        private void shutdown(ThreadGroup tg) {
            Thread[] threads = listThreads(tg);
            for (Thread th: threads) {
                if (th != null) {
                    th.interrupt();
                }
            }
        }

        private void join(ThreadGroup tg) {
            Thread[] threads = listThreads(tg);
            for (Thread th: threads) {
                if (th != null) {
                    try {
                        th.join(1000);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }

        private Thread[] listThreads(ThreadGroup tg) {
            Thread[] threads = new Thread[2 * tg.activeCount()];
            while(true) {
                int n = tg.enumerate(threads, true);
                if (n == 0) {
                    return new Thread[0];
                } else if (n < threads.length) {
                    return Arrays.copyOf(threads, n);
                } else {
                    // not enough space to enlist all threads
                    threads = new Thread[2 * threads.length];
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

    public void exec(String... cmd) {
        try {
            CommandLauncher cl = (CommandLauncher) mainClass.newInstance();
            cl.suppressSystemExit();
            StringBuilder sb = new StringBuilder();
            sb.append(mainClass.getSimpleName());
            for(String c: cmd) {
                sb.append(' ').append(escape(c));
            }
            System.out.println(sb);
            out.line(sb.toString());
            out.verify();
            Assert.assertTrue(cl.start(cmd));

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void fail(String... cmd) {
        try {
            CommandLauncher cl = (CommandLauncher) mainClass.newInstance();
            cl.suppressSystemExit();
            StringBuilder sb = new StringBuilder();
            sb.append(mainClass.getSimpleName());
            for(String c: cmd) {
                sb.append(' ').append(escape(c));
            }
            System.out.println(sb);
            out.line(sb.toString());
            out.verify();
            Assert.assertFalse(cl.start(cmd));

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object escape(String c) {
        if (c.split("\\s").length > 1) {
            return '\"' + c + '\"';
        }
        else {
            return c;
        }
    }
}
