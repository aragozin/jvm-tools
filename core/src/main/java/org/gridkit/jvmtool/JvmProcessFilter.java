package org.gridkit.jvmtool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gridkit.lab.jvm.attach.JavaProcessDetails;
import org.gridkit.lab.jvm.attach.JavaProcessMatcher;

import com.beust.jcommander.Parameter;

public class JvmProcessFilter implements JavaProcessMatcher {
	
	@Parameter(names = {"-df", "--description-filter"}, description = "Wild card expression to match process description")
	private String descFilter;

	@Parameter(names = {"-pf", "--property-filter"}, variableArity = true, description = "Wild card expressions to match JVM system properties")
	private List<String> propFilters;
	
	private Pattern descPattern;
	private List<Pattern> propPatterns; 
	
	public boolean isDefined() {
		return descFilter != null || propFilters != null;
	}

	public void prepare() {
		if (descFilter != null) {
			descPattern = GlobHelper.translate(descFilter, "\0");
		}
		if (propFilters != null) {
			propPatterns = new ArrayList<Pattern>();
			for(String pp: propFilters) {
				Pattern tp = GlobHelper.translate(pp, ".");
				propPatterns.add(tp);
			}
		}
	}
	
	@Override
	public boolean evaluate(JavaProcessDetails proc) {
		if (descPattern != null) {
			if (!descPattern.matcher(proc.getDescription()).matches()) {
				return false;
			}
		}
		
		if (propPatterns != null) {
			List<String> props = new ArrayList<String>();
			for(Map.Entry<Object, Object> e: proc.getSystemProperties().entrySet()) {
				props.add(e.getKey() + "=" + e.getValue());
			}
			
			filterLoop:
			for(Pattern pp: propPatterns) {
				for(String p: props) {
					if (pp.matcher(p).matches()) {
						continue filterLoop;
					}
				}
				return false;
			}
		}
		
		return true;
	}
}
