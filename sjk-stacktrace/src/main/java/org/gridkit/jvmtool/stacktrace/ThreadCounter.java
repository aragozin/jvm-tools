package org.gridkit.jvmtool.stacktrace;

public enum ThreadCounter {

    // Order of enum constants is used in wire thread dump encoding
    CPU_TIME,
    USER_TIME,
    ALLOCATED_BYTES,
    BLOCKED_TIME,
    BLOCKED_COUNTER,
    WAIT_TIME,
    WAIT_COUNTER,
}
