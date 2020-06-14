package org.gridkit.jvmtool;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface CheckDuration {

    /**
     * Command will run for N seconds then interrupted.
     */
    public long value();

}
