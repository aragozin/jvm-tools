package org.gridkit.jvmtool;

import org.apache.catalina.startup.Tomcat;

public class TomcatHelper {

    public static void startTomcat() {
        try {
            Tomcat tc = new Tomcat();
            tc.start();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
