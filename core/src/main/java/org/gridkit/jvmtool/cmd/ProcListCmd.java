package org.gridkit.jvmtool.cmd;

import java.io.PrintStream;
import java.util.List;

import org.gridkit.jvmtool.AttachUtil;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class ProcListCmd implements SimpleCommand {

	static {
        AttachUtil.ensureToolsClasspath();
    }	
	
	@Override
	public String getCommand() {
		return "ps";		
	}

	@Override
	public String getDescription() {
		return "List JVMs on local system";
	}

	@Override
	public void printUsage(PrintStream out) {
		out.println("ps");
	}

	@Override
	public void printHelp(PrintStream out) {
	}

	@Override
	public void exec(List<String> args) {
        for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
            System.out.print(vm.id());
            System.out.print("\t");
            System.out.print(vm.displayName());
            System.out.print("\n");
        }
    }	
	
	public static void main(String[] args) {
		new ProcListCmd().exec(null);
	}
}
