package org.gridkit.jvmtool.event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This utility class creates dynamic wrapper over {@link Event}
 * and replaces tags and counter with mutable versions;
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class EventDecorator {

    private long timestamp = -1;
    private SimpleTagCollection tags = new SimpleTagCollection();
    private SimpleCounterCollection counters = new SimpleCounterCollection();

    private Map<Class<?>, Event> wrapperCache = new HashMap<Class<?>, Event>();
    private InvocationHandler handler = new Handler();

    private Event delegate;

    public EventDecorator() {
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> T wrap(T event) {

        if (event == null) {
            return null;
        }

        timestamp = -1;
        tags.clear();
        counters.clear();
        delegate = event;

        if (event instanceof TimestampedEvent) {
            timestamp = ((TimestampedEvent) event).timestamp();

        }
        if (event instanceof MultiCounterEvent) {
            counters.setAll(((MultiCounterEvent) event).counters());
        }
        if (event instanceof TaggedEvent) {
            tags.putAll(((TaggedEvent) event).tags());
        }

        Event proxy = wrapperCache.get(event.getClass());
        if (proxy == null) {
            initProxy(event.getClass());
            proxy = wrapperCache.get(event.getClass());
        }

        return (T)proxy;
    }

    private void initProxy(Class<? extends Event> type) {
        List<Class<?>> facade = new ArrayList<Class<?>>();
        for(Class<?> c: type.getInterfaces()) {
            if (Event.class.isAssignableFrom(c)) {
                facade.add(c);
            }
        }

        if (facade.isEmpty()) {
            throw new IllegalArgumentException("Invalid event type: " + type.getClass());
        }

        opt:
        while(true) {
            for(Class<?> i: facade) {
                for(Class<?> j: facade) {
                    if (i != j && i.isAssignableFrom(j)) {
                        facade.remove(i);
                        continue opt;
                    }
                }
            }
            break;
        }

        Object proxy = Proxy.newProxyInstance(facade.get(0).getClassLoader(), facade.toArray(new Class<?>[0]), handler);

        wrapperCache.put(type, (Event)proxy);
    }

    public void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public SimpleTagCollection tags() {
        return this.tags;
    }

    public SimpleCounterCollection counters() {
        return this.counters;
    }

    private class Handler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args == null || args.length == 0) {
                if ("timestamp".equals(method.getName())) {
                    return timestamp;
                }
                else if ("tags".equals(method.getName())) {
                    return tags;
                }
                else if ("counters".equals(method.getName())) {
                    return counters;
                }
            }

            return method.invoke(delegate, args);
        }
    }
}
