package org.gridkit.jvmtool.cmd;

import java.util.List;

public interface SimpleCommand extends Command {

	public void exec(List<String> args);
	
}
