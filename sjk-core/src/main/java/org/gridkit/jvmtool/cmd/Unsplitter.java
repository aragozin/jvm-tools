package org.gridkit.jvmtool.cmd;

import java.util.Collections;
import java.util.List;

import com.beust.jcommander.converters.IParameterSplitter;

public class Unsplitter implements IParameterSplitter {

	@Override
	public List<String> split(String value) {
		return Collections.singletonList(value);
	}
}
