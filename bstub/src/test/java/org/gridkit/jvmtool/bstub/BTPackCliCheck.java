package org.gridkit.jvmtool.bstub;

import static org.gridkit.nanocloud.test.maven.MavenClasspathConfig.MAVEN;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;

import org.gridkit.jvmtool.bstub.BStub;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class BTPackCliCheck {

//    static {
//        BTWarmUp.prefetchClasses();
//    }
    
    Cloud cloud = CloudFactory.createCloud();
    {
        cloud.node("**").x(VX.TYPE).setLocal();
        cloud.node("**").x(MAVEN)
            .remove("org.gridkit.3rd.btrace", "asm")
            .remove("org.gridkit.3rd.btrace", "btrace-agent")
            .remove("org.gridkit.3rd.btrace", "btrace-boot")
            .remove("org.gridkit.3rd.btrace", "client")
            .remove("org.gridkit.3rd.btrace", "compiler")
            .remove("org.gridkit.3rd.btrace", "core")
            .remove("org.gridkit.3rd.btrace", "core-api")            
//            .remove("org.gridkit.3rd.btrace", "ext-collections")
            .remove("org.gridkit.3rd.btrace", "ext-default")                        
            .remove("org.gridkit.3rd.btrace", "instr")
            .remove("org.gridkit.3rd.btrace", "runtime")
        ;
    }
    
    @After
    public void cleanup() {
        cloud.shutdown();
    }
    
    private static String PID;
    static {
        PID = ManagementFactory.getRuntimeMXBean().getName();
        PID = PID.substring(0, PID.indexOf('@'));
    }

    static String getPID() {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        System.out.println(pid);
        pid = pid.substring(0, pid.indexOf('@'));
        return pid;
    }
    
    @Test
    public void help() {
        exec("--help");
    }

    @Test
    public void list_commands() {
        exec("--commands");
    }
    
    @Test
    public void stub() {
        ViNode node = cloud.node("slave");
        String pid = node.exec(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return getPID();
            }
        });
        node.submit(new Runnable() {
            
            @Override
            public void run() {
                new DummyWorker().start();                
            }
        });
        exec("stub", "-p", pid, "--bt-debug", "-s", "target/test-classes/org/gridkit/jvmtool/bstub/TestScript.class", "-X");
    }
    
    private void exec(String... cmd) {
        BStub sjk = new BStub();
        sjk.suppressSystemExit();
        StringBuilder sb = new StringBuilder();
        sb.append("BTPack");
        for(String c: cmd) {
            sb.append(' ').append(escape(c));
        }
        System.out.println(sb);
        Assert.assertTrue(sjk.start(cmd));      
    }

    @SuppressWarnings("unused")
    private void fail(String... cmd) {
        BStub sjk = new BStub();
        sjk.suppressSystemExit();
        StringBuilder sb = new StringBuilder();
        sb.append("BTPack");
        for(String c: cmd) {
            sb.append(' ').append(escape(c));
        }
        System.out.println(sb);
        Assert.assertFalse(sjk.start(cmd));     
    }

    private Object escape(String c) {
        if (c.split("\\s").length > 1) {
            return '\"' + c + '\"';
        }
        else {
            return c;
        }
    }       
}
