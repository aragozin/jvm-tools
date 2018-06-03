package org.gridkit.jvmtool.cli;

import java.io.IOException;
import java.util.List;

public class ProcessSpawner {

	public static void start(List<String> commands) {
		try {
			ProcessBuilder pb = new ProcessBuilder(commands);
			pb.inheritIO();
			System.exit(pb.start().waitFor());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(-1);
	}	
}
