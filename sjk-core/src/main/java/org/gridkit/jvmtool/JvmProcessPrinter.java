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

import java.util.List;

import org.gridkit.jvmtool.cli.CommandLauncher;
import org.gridkit.lab.jvm.attach.AttachManager;
import org.gridkit.lab.jvm.attach.JavaProcessId;
import org.gridkit.util.formating.GridSink;

import com.beust.jcommander.Parameter;

/**
 * Configurable process description builder
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class JvmProcessPrinter {

    @SuppressWarnings("unused")
    private final CommandLauncher host;

    @Parameter(names = {"-pd", "--process-details"}, variableArity = true, description = "Print custom information related to a process. Following tags can be used: PID, MAIN, FDQN_MAIN, ARGS, D<sys-prop>, d<sys-prop>, X<jvm-flag>, x<jvm-flag>")
    private List<String> displayFields;

    public JvmProcessPrinter(CommandLauncher host) {
        this.host = host;
    }

    public boolean isDefined() {
        return displayFields != null && !displayFields.isEmpty();
    }

    public void describeHeader(GridSink sink) {
        for(String tag: displayFields) {
            if (tag.startsWith("d") || tag.startsWith("x") || tag.startsWith("D") || tag.startsWith("X")) {
                sink.append(tag.substring(1));
            }
            else {
                sink.append(tag);
            }
        }
        sink.endOfRow();

    }

    public void describe(JavaProcessId jpid, GridSink sink) {
        for(String tag: displayFields) {
            if ("PID".equals(tag)) {
                sink.append(getPid(jpid));
            }
            else if ("MAIN".equals(tag)) {
                sink.append(getShortMain(jpid));
            }
            else if ("FDQN_MAIN".equals(tag)) {
                sink.append(getMain(jpid));
            }
            else if ("ARGS".equals(tag)) {
                sink.append(getArgs(jpid));
            }
            else if (tag.startsWith("d")) {
                sink.append(getProp(jpid, tag.substring(1)));
            }
            else if (tag.startsWith("D")) {
                sink.append(tag.substring(1) + "=" + getProp(jpid, tag.substring(1)));
            }
            else if (tag.startsWith("x")) {
                sink.append(getFlagValue(jpid, tag.substring(1)));
            }
            else if (tag.startsWith("X")) {
                sink.append(getFlag(jpid, tag.substring(1)));
            }
            else {
                sink.append("Unknown(" + tag + ")");
            }
        }
        sink.endOfRow();
    }

    private String getFlagValue(JavaProcessId jpid, String flagName) {
        String val = getFlag(jpid, flagName);
        if (val.startsWith("-XX:+")) {
            return "true";
        } else if (val.startsWith("-XX:-")) {
            return "false";
        } else {
            int ch = val.indexOf("=");
            return ch <= 0 ? val : val.substring(ch + 1);
        }
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
        if (main.endsWith(".jar")) {
            return main;
        }
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
