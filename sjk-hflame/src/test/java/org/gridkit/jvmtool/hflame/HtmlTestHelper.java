package org.gridkit.jvmtool.hflame;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

public class HtmlTestHelper {

	public static void openBrowser(String content) throws IOException {
		File file = new File("target/html_out/" + System.currentTimeMillis() + ".html");
		file.getParentFile().mkdirs();
		FileWriter fw = new FileWriter(file);
		fw.append(content);
		fw.close();
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            // Desktop.isDesktopSupported()
            Boolean supported = (Boolean) desktopClass.
                getMethod("isDesktopSupported").
                invoke(null, new Object[0]);
            URI uri = file.toURI();
            if (supported) {
                // Desktop.getDesktop();
                Object desktop = desktopClass.getMethod("getDesktop").
                    invoke(null, new Object[0]);
                // desktop.browse(uri);
                desktopClass.getMethod("browse", URI.class).
                    invoke(desktop, uri);
                return;
            }
        } catch (Exception e) {
            // ignore
        }
		
	}	
}
