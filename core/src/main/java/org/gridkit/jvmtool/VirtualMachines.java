// Copyright (c) 2012 Cloudera, Inc. All rights reserved.
package org.gridkit.jvmtool;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class VirtualMachines {
	
	static {
        AttachUtil.ensureToolsClasspath();
    }
	
    public void run() throws Exception {
        for (VirtualMachineDescriptor vm : VirtualMachine.list()) {
            System.out.print(vm.id());
            System.out.print("\t");
            System.out.print(vm.displayName());
            System.out.print("\n");
        }
    }
}
