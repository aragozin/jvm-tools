package org.gridkit.jvmtool.gcmon;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.event.TimespanEvent;

public interface GarbageCollectionEvent extends GarbageCollectionSummary, TimespanEvent, CommonEvent {

}
