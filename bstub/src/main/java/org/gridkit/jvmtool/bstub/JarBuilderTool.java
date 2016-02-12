package org.gridkit.jvmtool.bstub;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarBuilderTool {
    
    private Manifest mf = new Manifest();
    private List<String> packages = new ArrayList<String>();
    private List<String> services = new ArrayList<String>();

    private Set<String> pathEntries = new HashSet<String>();
    
    public Manifest getManifest() {
        return mf;
    }
    
    public void addPackage(String pkg) {
        packages.add(pkg.replace('.', '/'));
    }
    
    public void addService(String service) {
        services.add(service);
    }
    
    public byte[] collectJar() throws IOException {
        Manifest manifest = mf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        ZipOutputStream jarOut = new ZipOutputStream(bos);
        if (manifest != null) {
            ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
            e.setTime(0l); // this to ensure equal hash for equal content
            jarOut.putNextEntry(e);
            manifest.write(jarOut);
            jarOut.closeEntry();            
        }
        for(String srv: services) {
            processService(srv, jarOut);
        }
        for(String pkg: packages) {
            processPath(pkg, jarOut);
        }
        pathEntries.clear();
        jarOut.close();
        byte[] jarFile = bos.toByteArray();
        return jarFile;
    }
    
    private void processService(String srv, ZipOutputStream jarOut) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Set<String> imps = new TreeSet<String>();
        
        String sd = "META-INF/services/" + srv;
        Enumeration<URL> en = cl.getResources(sd);
        while(en.hasMoreElements()) {
            URL url = en.nextElement();
            Collection<String> c = toLines(url.openStream());
            imps.addAll(c);
        }

        StringBuilder sb = new StringBuilder();
        for(String s: imps) {
            s = s.trim();
            if (!s.isEmpty()) {
                sb.append(s).append('\n');
            }
        }

        ZipEntry entry = new ZipEntry(sd);
        entry.setTime(0); // this is to facilitate content cache
        if (!pathEntries.contains(entry.getName())) {
            pathEntries.add(entry.getName());
            jarOut.putNextEntry(entry);
            jarOut.write(sb.toString().getBytes(Charset.forName("UTF8")));
            jarOut.closeEntry();                            
        }        
    }

    private void processPath(String path, ZipOutputStream jarOut) throws MalformedURLException, IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String basePackage = path;
        Enumeration<URL> en = cl.getResources(path);
        while(en.hasMoreElements()) {
            URL url = en.nextElement(); 
            String urlp = url.toExternalForm();
            if (urlp.indexOf('?') > 0) {
                urlp = urlp.substring(0, urlp.indexOf('?'));
            }
            addFiles(jarOut, basePackage, urlp);                    
        }
    }

//    private static byte[] jarFiles(String path) throws IOException {
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        JarOutputStream jarOut = new JarOutputStream(bos);
//        int size = addFiles(jarOut, "", new File(path));
//        if (size == 0) {
//            // no files in folder
//            return null;
//        }
//        jarOut.close();
//        return bos.toByteArray();
//    }
//
//    private static int addFiles(JarOutputStream jarOut, String base, File path) throws IOException {
//        int count = 0;
//        for(File file : path.listFiles()) {
//            if (file.isDirectory()) {
//                final String dirName = base + file.getName() + "/";
//
//                JarEntry entry = new JarEntry(dirName);
//                entry.setTime(0l);// this to ensure equal hash for equal content
//                jarOut.putNextEntry(entry);
//                jarOut.closeEntry();
//                count += addFiles(jarOut, dirName, file);
//            }
//            else {
//                JarEntry entry = new JarEntry(base + file.getName());
//                entry.setTime(file.lastModified());
//                jarOut.putNextEntry(entry);
//                copyStream(new FileInputStream(file), jarOut);
//                jarOut.closeEntry();
//                ++count; 
//            }
//        }
//        return count;
//    }

    private void addFiles(ZipOutputStream jarOut, String basePackage, String baseUrl) throws IOException, MalformedURLException {
        if (baseUrl.startsWith("jar:")) {
            int n = baseUrl.lastIndexOf("!");
            if (n < 0) {
                throw new IllegalArgumentException("Unexpected classpath URL: " + baseUrl);
            }
            String fileUrl = baseUrl.substring(4, n);
            InputStream is = new URL(fileUrl).openStream();
            ZipInputStream zis = new ZipInputStream(is);
            while(true) {
                ZipEntry ze = zis.getNextEntry();
                if (ze != null) {
                    if (matchPath(ze.getName(), basePackage)) {
                        ZipEntry entry = new ZipEntry(ze.getName());
                        entry.setTime(0); // this is to facilitate content cache
                        if (!pathEntries.contains(entry.getName())) {
                            pathEntries.add(entry.getName());
                            jarOut.putNextEntry(entry);
                            copyStreamNoClose(zis, jarOut);
                            jarOut.closeEntry();                            
                        }
                    }
                    zis.closeEntry();
                }
                else {
                    break;
                }               
            }
        }
        else {
            InputStream is = new URL(baseUrl).openStream();
            for(String line: toLines(is)) {
                String fpath = baseUrl + "/" + line;
                String jpath = basePackage + "/" + line;
                ZipEntry entry = new ZipEntry(jpath);
                entry.setTime(0); // this is to facilitate content cache            
                if (!pathEntries.contains(entry.getName())) {
                    jarOut.closeEntry();                            
                    jarOut.putNextEntry(entry);
                    copyStream(new URL(fpath).openStream(), jarOut);
                    jarOut.closeEntry();
                }
            }
        }
    }
    
    private boolean matchPath(String path, String basePath) {
        return path.startsWith(basePath) && path.lastIndexOf('/') <= basePath.length();
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
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
    
    private static Collection<String> toLines(InputStream is) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> result = new ArrayList<String>();
            while(true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
            return result;
        }
        finally {
            try {
                is.close();
            }
            catch(Exception e) {
                // ignore
            }
        }
    }
    
    private static void copyStreamNoClose(InputStream in, OutputStream out) throws IOException {
        boolean doClose = true;
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
            doClose = false;
            
        } finally {
            if (doClose) {
                // close if there were exception thrown
                try {
                    in.close();
                }
                catch(Exception e) {
                    // ignore
                }
            }
        }
    }   
}
