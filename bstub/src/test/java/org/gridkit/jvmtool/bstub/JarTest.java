package org.gridkit.jvmtool.bstub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.gridkit.jvmtool.bstub.AgentHelper;
import org.junit.Test;

public class JarTest {

    @Test
    public void verify_agent_generation() throws IOException {
        String path = AgentHelper.initAgentJar();
        System.out.println(path);
    }
    
    @Test
    public void verify_generic_jar() throws IOException {
        
        byte[] data = AgentHelper.createAgentJar();
        File out = new File("target/jartest/synthjar.jar");
        out.delete();
        out.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(data);
        fos.close();
    }    
}
