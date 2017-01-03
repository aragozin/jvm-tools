package org.gridkit.jvmtool.jvmevents;

public class JvmEvents {

    public static final String JVM_EVENT_KEY = "jvm.event";

    public static final String EVENT_THREAD_SNAPSHOT = "jvm.thread-snapshot";
    public static final String EVENT_GC = "jvm.event.gc";
    public static final String EVENT_STW = "jvm.event.stw";

    public static final String GC_NAME = "jvm.gc.name";
    public static final String GC_MEMORY_SPACES = "jvm.gc.memory-spaces";
    public static final String GC_COUNT = "jvm.gc.count";
    public static final String GC_TOTAL_TIME_MS = "jvm.gc.total-time";

    public static final String MEM_SPACE_INFO = "jvm.memory-space";

    public static final String THREAD_ID = "thread.javaId";
    public static final String THREAD_NAME = "thread.javaName";
    public static final String THREAD_STATE = "thread.javaState";

    public static final String memorySpaceName(String space) {
        return MEM_SPACE_INFO + "." + space + ".name";
    }

    public static final String memorySpaceUsed(String space) {
        return MEM_SPACE_INFO + "." + space + ".used";
    }

    public static final String memorySpaceBefore(String space) {
        return MEM_SPACE_INFO + "." + space + ".before";
    }

    public static final String memorySpaceMax(String space) {
        return MEM_SPACE_INFO + "." + space + ".max";
    }
}
