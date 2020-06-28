/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gridkit.lab.jvm.attach.JavaProcessDetails;
import org.gridkit.lab.jvm.attach.JavaProcessMatcher;

import com.beust.jcommander.Parameter;

/**
 * Configurable Java process filter.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmProcessFilter implements JavaProcessMatcher {

    @Parameter(names = {"-fd", "--filter-description"}, description = "Wild card expression to match process description")
    private String descFilter;

    @Parameter(names = {"-fp", "--filter-property"}, variableArity = true, description = "Wild card expressions to match JVM system properties")
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
