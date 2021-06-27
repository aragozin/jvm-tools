package org.gridkit.sjk.test.console;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.gridkit.sjk.test.console.junit4.CliTestRule;

/**
 * Annotation is used with {@link CliTestRule}. CLI execution will be interrupted
 * after given time in seconds. Useful for testing commands which do not exit naturally.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface StopCommandAfter {

    /**
     * Command will run for N seconds then interrupted.
     */
    public long value();

}
