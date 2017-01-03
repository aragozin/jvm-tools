package org.gridkit.jvmtool.codec.stacktrace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.gridkit.jvmtool.codec.stacktrace.EventEqualToCondition.countersToString;

import java.util.Arrays;
import java.util.Iterator;

import org.assertj.core.api.Condition;
import org.gridkit.jvmtool.event.ErrorEvent;
import org.gridkit.jvmtool.event.Event;
import org.gridkit.jvmtool.event.EventReader;
import org.gridkit.jvmtool.event.MultiCounterEvent;
import org.gridkit.jvmtool.event.TaggedEvent;
import org.gridkit.jvmtool.event.TimestampedEvent;
import org.gridkit.jvmtool.stacktrace.StackFrameList;

public class EventSeqEqualToCondition extends Condition<Iterable<Event>> {

    public static EventSeqEqualToCondition exactlyAs(Event... e) {
        return new EventSeqEqualToCondition(e);
    }

    public static EventSeqEqualToCondition exactlyAs(EventReader<Event> reader) {
        return new EventSeqEqualToCondition(reader);
    }

    private Event[] events;
    private EventReader<Event> reader;

    public EventSeqEqualToCondition(Event... expected) {
        this.events = expected;
    }

    public EventSeqEqualToCondition(EventReader<Event> expected) {
        this.reader = expected;
    }

    @Override
    public boolean matches(Iterable<Event> value) {
        Iterator<Event> e = reader == null ? Arrays.asList(events).iterator() : reader.iterator();
        Iterator<Event> a = value.iterator();

        while(a.hasNext() && e.hasNext()) {
            matches(e.next(), a.next());
        }

        if (a.hasNext()) {
            fail("More than expected events");
        }
        if (e.hasNext()) {
            fail("Less than expected events");
        }

        return true;
    }

    private boolean matches(Event expected, Event value) {

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
                fail("Event " + value + " should not be ErrorEvent", ((ErrorEvent) value).exception());
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
}
