package org.gridkit.jvmtool.bstub;

public class DummyWorker extends Thread {

    public void run() {
        while(true) {
            try {
                Thread.sleep(300);
                doSomethingStupid();
            } catch (InterruptedException e) {               
            }
        }
    }
    
    public void doSomethingStupid() {
        System.out.println("doSomethingStupid");        
    }
    
}
