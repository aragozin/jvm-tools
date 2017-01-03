package org.gridkit.jvmtool.event;

public interface EventTransformer<A extends Event, B extends Event> {

    public B transform(A source);

}
