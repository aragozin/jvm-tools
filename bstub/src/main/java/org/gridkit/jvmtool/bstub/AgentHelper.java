package org.gridkit.jvmtool.bstub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class AgentHelper {

    private static File agentJar; 
    
    public static synchronized String initAgentJar() throws IOException {
        if (agentJar != null) {
            return agentJar.getCanonicalPath();
        }
        
        File t = File.createTempFile("___", "_");
        File tdir = t.getParentFile();
        File tt = new File(tdir, "btpack");
        tt.mkdir();
        
        byte[] agent = createAgentJar();
        
        String hash = digest(agent, "SHA-256");
        
        File name = new File(tt, "agent-" + hash + ".jar");

        if (name.isFile() && name.length() == agent.length) {
            agentJar = name;
            return name.getCanonicalPath();
        }
        
        name.delete();
        
        FileOutputStream fos = new FileOutputStream(name);
        fos.write(agent);
        fos.close();
        
        agentJar = name;
        
        return name.getCanonicalPath();
    }

    static byte[] createAgentJar() throws IOException {
        
        JarBuilderTool builder = new JarBuilderTool();
        Manifest mf = builder.getManifest();
        
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Attributes.Name("Agent-Class"), "net.java.btrace.agent.Main");
        mf.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "true");
        mf.getMainAttributes().put(new Attributes.Name("Can-Retransform-Classes"), "true");
        
        builder.addService("net.java.btrace.spi.wireio.CommandImpl");
        builder.addService("net.java.btrace.spi.server.ServerImpl");
        
        builder.addPackage("net.java.btrace.agent");   
        builder.addPackage("net.java.btrace.agent.wireio");   
        builder.addPackage("net.java.btrace.annotations");   
        builder.addPackage("net.java.btrace.api.core");   
        builder.addPackage("net.java.btrace.api.extensions");   
        builder.addPackage("net.java.btrace.api.extensions.runtime");   
        builder.addPackage("net.java.btrace.api.extensions.util");   
        builder.addPackage("net.java.btrace.api.server");
        builder.addPackage("net.java.btrace.api.types");
        builder.addPackage("net.java.btrace.api.wireio");
        builder.addPackage("net.java.btrace.commands");   
        builder.addPackage("net.java.btrace.ext");   
        builder.addPackage("net.java.btrace.instr");   
        builder.addPackage("net.java.btrace.org.objectweb.asm");   
        builder.addPackage("net.java.btrace.org.objectweb.asm.commons");   
        builder.addPackage("net.java.btrace.org.objectweb.asm.signature");   
        builder.addPackage("net.java.btrace.resources");   
        builder.addPackage("net.java.btrace.runtime");   
        builder.addPackage("net.java.btrace.server");   
        builder.addPackage("net.java.btrace.spi");   
        builder.addPackage("net.java.btrace.spi.server");   
        builder.addPackage("net.java.btrace.spi.wireio");   
        builder.addPackage("net.java.btrace.util");
        builder.addPackage("net.java.btrace.server.wireio");   
        builder.addPackage("net.java.btrace.wireio.commands");   

        builder.addPackage("org.gridkit.nimble.btrace.ext");   
        
        byte[] data = builder.collectJar();
        
        return data;
    }

    private static String digest(byte[] data, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(data);
            StringBuilder buf = new StringBuilder();
            for(byte b: digest) {
                buf.append(Integer.toHexString(0xF & (b >> 4)));
                buf.append(Integer.toHexString(0xF & (b)));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
