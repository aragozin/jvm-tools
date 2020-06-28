package org.gridkit.jvmtool.agent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

public class SjkAgentLocatorCheck {

    @Test
    public void checkZipURL() {
        URL url = SjkAgent.class.getClassLoader().getResource("java/lang/String.class");
        System.out.println(url.toString());
    }

    @Test
    public void checkFileURL() {
        URL url = SjkAgent.class.getClassLoader().getResource("org/gridkit/jvmtool/agent/SjkAgent.class");
        System.out.println(url.toString());
    }

    @Test
    public void checkLocator() throws IOException, URISyntaxException {
        System.out.println(SjkAgentLocator.getJarPath());
    }

}
