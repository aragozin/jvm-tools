package org.gridkit.jvmtool;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import sun.tools.attach.HotSpotVirtualMachine;

import com.sun.tools.attach.VirtualMachine;


public class ClassHisto {

	public static void main(String[] args) throws Exception {
		
//		BugSpotAgent agent = new BugSpotAgent();
//		agent.attach(5296);
//		
//		System.out.println("Attached!");
//		JMap.main(new String[]{"-histo:live", "5460"});
		
		long start = System.nanoTime();
		HotSpotVirtualMachine vm = (HotSpotVirtualMachine) AttachUtil.attachToPid("5460");
		for(int i = 0; i != 1; ++i) {
//			InputStream is = vm.heapHisto("-all");
			InputStream is = vm.printFlag("NewSize");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			while(true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				System.out.println(line);
			}
		}
		System.out.println("100 dumps in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + "ms");
	}
	
}
