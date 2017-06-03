package org.gridkit.jvmtool;

import java.util.List;

import org.gridkit.jvmtool.cli.CommandLauncher;

import com.beust.jcommander.Parameter;

public class ThreadDumpSource extends AbstractThreadDumpSource {

    @Parameter(names={"-f", "--file"}, required = false, variableArity=true, description="Path to stack dump file")
    private List<String> files;
    
    
    public ThreadDumpSource(CommandLauncher host) {
        super(host);
    }


	@Override
	protected List<String> inputFiles() {
		return files;
	}
}
