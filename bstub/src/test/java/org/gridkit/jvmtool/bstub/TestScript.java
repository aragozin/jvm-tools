package org.gridkit.jvmtool.bstub;

import net.java.btrace.annotations.BTrace;
import net.java.btrace.annotations.OnError;
import net.java.btrace.annotations.OnMethod;
import net.java.btrace.annotations.ProbeClassName;
import net.java.btrace.annotations.ProbeMethodName;
import net.java.btrace.ext.Printer;

@BTrace
public class TestScript {

    @OnMethod(
        clazz = "/.*DummyWorker/",
        method = "doSomethingStupid"
    )
    public static void stubIn(@ProbeClassName String cname, @ProbeMethodName String mname) {
//        System.err.println("Catch: " + cname + "." + mname);
        Printer.println("Catch: " + cname + "." + mname);
    }
    
    @OnError
    public static void report() {
        Printer.print("Error occured");
    }
}
