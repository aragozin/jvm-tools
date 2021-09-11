package org.gridkit.jvmtool.agent;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModuleHack {

    public static void extendAccess(Instrumentation insrt) throws Exception {
        addExport(insrt, moduleOf(String.class), "jdk.internal.vm", moduleOf(ModuleHack.class));
    }

    @SuppressWarnings("unchecked")
    private static void addExport(Instrumentation insrt, Module from, String pn, Module to) {
        Map<String, Set<Module>> extraExports = new HashMap<String, Set<Module>>();
        extraExports.put(pn, Collections.singleton(to));
        insrt.redefineModule(from, Collections.EMPTY_SET, extraExports, Collections.EMPTY_MAP, Collections.EMPTY_SET, Collections.EMPTY_MAP);
    }

    private static Module moduleOf(Class<?> type) throws Exception {
        return (Module)type.getClass().getMethod("getModule").invoke(type);
    }

}
