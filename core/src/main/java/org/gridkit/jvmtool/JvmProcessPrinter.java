package org.gridkit.jvmtool;

import java.util.List;

import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;

import com.beust.jcommander.Parameter;

public class JvmProcessPrinter {
	
	@Parameter(names = {"-dp", "--describe-process"}, variableArity = true, description = "Print custom information related to a process. Following tags can be used: PID, MAIN, FDQN_MAIN, ARGS, D<sys-prop>, d<sys-prop>, X<jvm-flag>")
	private List<String> displayFields;
	
	public boolean isDefined() {
		return displayFields != null && !displayFields.isEmpty();
	}

	public String describe(JavaProcessId jpid) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(String tag: displayFields) {
			if (!first) {
				sb.append('\t');
			}
			first = false;
			if ("PID".equals(tag)) {
				sb.append(getPid(jpid));
			}
			else if ("MAIN".equals(tag)) {
				sb.append(getShortMain(jpid));
			}
			else if ("FDQN_MAIN".equals(tag)) {
				sb.append(getMain(jpid));
			}
			else if ("ARGS".equals(tag)) {
				sb.append(getArgs(jpid));
			}
			else if (tag.startsWith("d")) {
				sb.append(getProp(jpid, tag.substring(1)));
			}
			else if (tag.startsWith("D")) {
				sb.append(tag.substring(1)).append("=");
				sb.append(getProp(jpid, tag.substring(1)));
			}
			else if (tag.startsWith("X")) {
				sb.append(getFlag(jpid, tag.substring(1)));
			}
			else {
				sb.append("Unknown(" + tag + ")");
			}
		}
		
		return sb.toString();
	}

	private String getProp(JavaProcessId jpid, String propName) {
		return (String) AttachManager.getDetails(jpid).getSystemProperties().get(propName);
	}

	private String getFlag(JavaProcessId jpid, String flagName) {
		return (String) AttachManager.getDetails(jpid).getVmFlag(flagName);
	}

	private String getArgs(JavaProcessId jpid) {
		String desc = jpid.getDescription();		
		int n = desc.indexOf(' ');
		if (n >= 0) {
			desc = desc.substring(n + 1);
		}
		return desc;
	}

	private String getMain(JavaProcessId jpid) {
		String desc = jpid.getDescription();		
		int n = desc.indexOf(' ');
		if (n >= 0) {
			desc = desc.substring(0, n);
		}
		return desc;
	}

	private String getShortMain(JavaProcessId jpid) {
		String main = getMain(jpid);
		int n = main.lastIndexOf('.');
		if (n >= 0) {
			main = main.substring(n + 1);
		}
		return main;
	}

	private String getPid(JavaProcessId jpid) {
		return String.valueOf(jpid.getPID());
	}
}
