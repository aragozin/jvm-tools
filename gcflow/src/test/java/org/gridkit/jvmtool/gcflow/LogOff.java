package org.gridkit.jvmtool.gcflow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.LogManager;

public class LogOff {

    public LogOff() throws IOException {
        Properties prop = new Properties();
        prop.setProperty(".level", "OFF");
        prop.setProperty(".level", "OFF");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        prop.store(bos, "");
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        LogManager.getLogManager().readConfiguration(bis);
    }
}
