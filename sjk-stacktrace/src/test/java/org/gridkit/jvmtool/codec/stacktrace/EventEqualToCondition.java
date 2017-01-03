package org.gridkit.jvmtool.codec.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Condition;
import org.gridkit.jvmtool.event.ErrorEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.MultiCounterEvent;
import org.gridkit.jvmtool.event.TaggedEvent;
import org.gridkit.jvmtool.event.TimestampedEvent;
import org.gridkit.jvmtool.stacktrace.CounterCollection;
import org.gridkit.jvmtool.stacktrace.StackFrameList;

public class EventEqualToCondition extends Condition<Event> {

    public static EventEqualToCondition eventEquals(Event e) {
        return new EventEqualToCondition(e);
    }

    Event expected;

    public EventEqualToCondition(Event expected) {
        super();
        this.expected = expected;
    }

    @Override
    public boolean matches(Event value) {

        if (expected instanceof ErrorEvent) {
            if (value instanceof ErrorEvent) {
                assertThat(((ErrorEvent) value).exception().toString()).isEqualTo(((ErrorEvent) expected).exception().toString());
            }
            else {
                fail("Event " + value + " should be ErrorEvent");
            }
        }
        else {
            if (value instanceof ErrorEvent) {
                fail("Event " + value + " should not be ErrorEvent");
            }
        }

        if (expected instanceof TimestampedEvent) {
            if (value instanceof TimestampedEvent) {
                assertThat(((TimestampedEvent) value).timestamp()).isEqualTo(((TimestampedEvent) expected).timestamp());
            }
            else {
                fail("Event " + value + " should be TimestampedEvent");
            }
        }
        else {
            if (value instanceof TimestampedEvent) {
                fail("Event " + value + " should not be TimestampedEvent");
            }
        }

        if (expected instanceof TaggedEvent) {
            if (value instanceof TaggedEvent) {
                assertThat(((TaggedEvent) value).tags().toString()).isEqualTo(((TaggedEvent) expected).tags().toString());
            }
            else {
                fail("Event " + value + " should be TaggedEvent");
            }
        }
        else {
            if (value instanceof TaggedEvent) {
                fail("Event " + value + " should not be TaggedEvent");
            }
        }

        if (expected instanceof MultiCounterEvent) {
            if (value instanceof MultiCounterEvent) {
                assertThat(countersToString(((MultiCounterEvent) value).counters())).isEqualTo(countersToString(((MultiCounterEvent) expected).counters()));
            }
            else {
                fail("Event " + value + " should be MultiCounterEvent");
            }
        }
        else {
            if (value instanceof MultiCounterEvent) {
                fail("Event " + value + " should not be MultiCounterEvent");
            }
        }

        if (expected instanceof ThreadSnapshotEvent) {
            if (value instanceof ThreadSnapshotEvent) {
                assertThat(((ThreadSnapshotEvent) value).threadId()).isEqualTo(((ThreadSnapshotEvent) expected).threadId());
                assertThat(((ThreadSnapshotEvent) value).threadName()).isEqualTo(((ThreadSnapshotEvent) expected).threadName());
                assertThat(((ThreadSnapshotEvent) value).threadState()).isEqualTo(((ThreadSnapshotEvent) expected).threadState());
                StackFrameList etrace = ((ThreadSnapshotEvent) expected).stackTrace();
                StackFrameList atrace = ((ThreadSnapshotEvent) value).stackTrace();
                if (etrace == null) {
                    assertThat(atrace).isNull();
                }
                else if (atrace == null) {
                    fail("Stack trace should not be null");
                }
                if (etrace != null) {
                    assertThat(atrace.toArray()).containsExactly(etrace.toArray());
                }
            }
            else {
                fail("Event " + value + " should be ThreadSnapshotEvent");
            }
        }
        else {
            if (value instanceof ThreadSnapshotEvent) {
                fail("Event " + value + " should not be ThreadSnapshotEvent");
            }
        }

        return true;
    }

    public static String countersToString(CounterCollection col) {
        List<String> names = new ArrayList<String>();
        for(String key: col) {
            names.add(key);
        }
        Collections.sort(names);
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(String key: names) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(key).append(": ").append(col.getValue(key));
        }
        sb.append(']');
        return sb.toString();
    }

}
