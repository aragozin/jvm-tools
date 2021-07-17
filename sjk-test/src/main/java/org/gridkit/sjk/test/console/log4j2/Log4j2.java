package org.gridkit.sjk.test.console.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

public class Log4j2 {

    public static void reconfigure() {
        // reconfigure log4j to log into new streams
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
        ctx.updateLoggers();
    }
}
