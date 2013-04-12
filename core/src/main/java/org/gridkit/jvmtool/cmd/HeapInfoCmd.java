package org.gridkit.jvmtool.cmd;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.gridkit.jvmtool.InvalidCommandLineException;
import org.gridkit.jvmtool.jmx.beans.GarbageCollectorMXStruct;
import org.gridkit.jvmtool.jmx.beans.MemoryPoolMXStruct;

public class HeapInfoCmd implements MBeanCommand {

	@Override
	public String getCommand() {
		return "meminfo";
	}

	@Override
	public String getDescription() {
		return "Display memory and GC configuration summary";
	}

	@Override
	public void printUsage(PrintStream out) {
		out.println("meminfo [<pid>|<host:port>]");
	}

	@Override
	public void printHelp(PrintStream out) {
	}

	@Override
	public void exec(MBeanServerConnection connection, List<String> args) {
		try {
			
//			MemoryMXStruct membean = MemoryMXStruct.get(connection);
			Map<String, GarbageCollectorMXStruct> gcbeans = GarbageCollectorMXStruct.get(connection);
			Map<String, MemoryPoolMXStruct> mpbeans = MemoryPoolMXStruct.get(connection);
			
			System.out.println("\nCollectors:");
			for(GarbageCollectorMXStruct gc: gcbeans.values()) {
				StringBuilder sb = new StringBuilder();
				sb.append("  ");
				sb.append(gc.getName());
				sb.append(" ").append(Arrays.toString(gc.getMemoryPoolNames()));				
				System.out.println(sb.toString());
			}
			
			System.out.println("\nMemory pools (heap):");
			for(MemoryPoolMXStruct mp: mpbeans.values()) {
				if (!"HEAP".equals(mp.getType())) {
					continue;
				}
				StringBuilder sb = new StringBuilder();
				sb.append("  ");
				sb.append(mp.getName());
				sb.append(" ").append(mp.getUsage());
				System.out.println(sb.toString());
			}

			System.out.println("\nMemory pools (non heap):");
			for(MemoryPoolMXStruct mp: mpbeans.values()) {
				if ("HEAP".equals(mp.getType())) {
					continue;
				}
				StringBuilder sb = new StringBuilder();
				sb.append("  ");
				sb.append(mp.getName());
				sb.append(" ")
					.append(mp.getType()).append(" ")
					.append(mp.getUsage());
				System.out.println(sb.toString());
			}
			
		} catch (Exception e) {
			System.err.println("JMX error: " + e.toString());
		}		
	}

	public static void main(String[] args) throws InvalidCommandLineException, IOException {
		new HeapInfoCmd().exec(ManagementFactory.getPlatformMBeanServer(), Arrays.asList(args));
	}	
}
