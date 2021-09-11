package org.gridkit.jvmtool.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SjkAgentLocator {

    private static final Class<?> ANCHOR_CLASS = SjkAgent.class;

    public static String getJarPath() throws IOException {
        try {
            URL url = ANCHOR_CLASS.getClassLoader().getResource(ANCHOR_CLASS.getName().replace('.',  '/') + ".class");
            if (url == null) {
                throw new RuntimeException("Failed to locate jar path");
            }
            if (url.getProtocol().equals("jar") || url.getProtocol().equals("zip")) {
                String path = url.toString();
                path = path.substring("jar:".length());
                int ch = path.lastIndexOf('!');
                if (ch > 0) {
                    path = path.substring(0, ch);
                }
                try {
                    URI jarUri = new URI(path);
                    File file = new File(jarUri);
                    if (file.isFile()) {
                        return file.getAbsolutePath();
                    } else {
                        throw new RuntimeException("Failed to locate jar path");
                    }
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Failed to locate jar path");
                }
            } else {
                File file = new File(url.toURI());
                if (!file.isFile()) {
                    throw new RuntimeException("Failed to locate jar path");
                }
                File root = file;
                String[] fqn = ANCHOR_CLASS.getName().split("[.]");
                for (int i = 0; i != fqn.length; ++i) {
                    root = root.getParentFile();
                }
                return createTmpJar(root);
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static String createTmpJar(File root) throws IOException {
        File targetJar;
        if (new File("target").isDirectory()) {
            // build environment
            new File("target/sjk-agent").mkdirs();
            targetJar = new File("target/sjk-agent/sjk-agent-" + System.currentTimeMillis() + ".jar");
        } else {
            targetJar = File.createTempFile("sjk-agent", ".jar");
        }

        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Attributes.Name("Agent-Class"), ANCHOR_CLASS.getName());
        mf.getMainAttributes().put(new Attributes.Name("Premain-Class"), ANCHOR_CLASS.getName());
        mf.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "false");

        FileOutputStream fos = new FileOutputStream(targetJar);
        fos.write(createBootstrapperJar(mf, root));
        fos.close();
        return targetJar.getAbsolutePath();
    }

    private static byte[] createBootstrapperJar(Manifest manifest, File root) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream jarOut = new ZipOutputStream(bos);
        if (manifest != null) {
            ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
            e.setTime(0l); // this to ensure equal hash for equal content
            jarOut.putNextEntry(e);
            manifest.write(jarOut);
            jarOut.closeEntry();
        }
        addFiles(jarOut, root, "");
        jarOut.close();
        byte[] jarFile = bos.toByteArray();
        return jarFile;
    }

    private static void addFiles(ZipOutputStream jarOut, File path, String prefix) throws IOException, MalformedURLException {
        File[] list = path.listFiles();
        for(File fpath: list) {
            String jpath = prefix + fpath.getName();
            if (fpath.isDirectory()) {
                addFiles(jarOut, fpath, jpath + "/");
            } else {
                if (jpath.equals(JarFile.MANIFEST_NAME)) {
                    continue;
                }
                ZipEntry entry = new ZipEntry(jpath);
                entry.setTime(0); // this is to facilitate content cache
                jarOut.putNextEntry(entry);
                copy(new FileInputStream(fpath), jarOut);
                jarOut.closeEntry();
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[1 << 12];
            while(true) {
                int n = in.read(buf);
                if(n >= 0) {
                    out.write(buf, 0, n);
                }
                else {
                    break;
                }
            }
        } finally {
            try {
                in.close();
            }
            catch(Exception e) {
                // ignore
            }
        }
    }
}
