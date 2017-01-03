package org.gridkit.jvmtool.event;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypedEventWriterProxy {

    public static <T extends TypedEventWriter> T createWriter(Class<T> facade, TypedEventWriterProvider provider) {
        return facade.cast(Proxy.newProxyInstance(facade.getClassLoader(), new Class<?>[]{facade}, new Handler(facade, provider)));
    }

    public static WriterBuilder decorate(UniversalEventWriter writer) {
        return new WriterBuilder(writer);
    }

    private static class Handler implements InvocationHandler {

        private final Class<?> facade;
        private final TypedEventWriterProvider provider;
        private final Map<Method, UniversalEventWriter> methodMap = new HashMap<Method, UniversalEventWriter>();

        public Handler(Class<?> facade, TypedEventWriterProvider provider) {
            this.facade = facade;
            this.provider = provider;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args); // delegate
            }
            else if ("close".equals(method.getName()) && method.getParameterTypes().length == 0) {
                provider.close();
            }
            else {
                UniversalEventWriter writer = writerFor(method);
                writer.store((Event) args[0]);
            }
            return null;
        }

        private UniversalEventWriter writerFor(Method method) {
            if (methodMap.containsKey(method)) {
                return methodMap.get(method);
            }
            else {
                if (method.getParameterTypes().length == 1
                        && Event.class.isAssignableFrom(method.getParameterTypes()[0])
                        && method.getParameterTypes()[0].isInterface()
                        && Event.class.isAssignableFrom(method.getParameterTypes()[0])
                        && method.getReturnType() == void.class) {

                    Class<?> type = method.getParameterTypes()[0];
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    UniversalEventWriter writer = provider.getWriterFor((Class)type);
                    methodMap.put(method, writer);
                    return writer;
                }
                else {
                    throw new IllegalArgumentException("Invalid event write method " + method.getName());
                }
            }
        }

        @Override
        public String toString() {
            return facade.getSimpleName() + "(" + provider + ")";
        }
    }

    private static class GenericEventWriterProvider implements TypedEventWriterProvider {

        private Class<?>[] eventTypes = new Class<?>[0];
        private UniversalEventWriter[] writers = new UniversalEventWriter[0];

        @Override
        public <T extends Event> UniversalEventWriter getWriterFor(Class<T> eventInterface) {
            int n = 0;
            for(Class<?> c: eventTypes) {
                if (c.isAssignableFrom(eventInterface)) {
                    return writers[n];
                }
                ++n;
            }
            throw new IllegalArgumentException("Event type " + eventInterface.getSimpleName() + " is not supported");
        }

        @Override
        public void close() {
            for(UniversalEventWriter writer: writers) {
                try {
                    writer.close();
                }
                catch(Exception e) {
                }
            }
        }

        public void add(Class<?> facade, UniversalEventWriter writer) {
            int n = eventTypes.length;
            eventTypes = Arrays.copyOf(eventTypes, n + 1);
            writers = Arrays.copyOf(writers, n + 1);

            eventTypes[n] = facade;
            writers[n] = writer;
        }
    }

    private static class TransformingWriter implements UniversalEventWriter {

        private final UniversalEventWriter nested;
        EventTransformer<Event, Event> transformer;

        public TransformingWriter(UniversalEventWriter nested, EventTransformer<Event, Event> transformer) {
            this.nested = nested;
            this.transformer = transformer;
        }

        @Override
        public void store(Event event) throws IOException {
            event = transformer == null ? event : transformer.transform(event);
            if (event != null) {
                nested.store(event);
            }
        }

        @Override
        public void close() throws IOException {
            nested.close();
        }
    }

    private static class ChainTransformer implements EventTransformer<Event, Event> {

        private final EventTransformer<Event, Event>[] tchain;

        public ChainTransformer(EventTransformer<Event, Event>[] tchain) {
            this.tchain = tchain;
        }

        @Override
        public Event transform(Event source) {
            Event e = source;
            for(int i = tchain.length - 1; i >= 0; --i) {
                e = tchain[i].transform(e);
                if (e == null) {
                    return null;
                }
            }
            return e;
        }
    }

    public static class WriterBuilder {

        private final List<EventTransformer<Event, Event>> tailTransformers = new ArrayList<EventTransformer<Event, Event>>();
        private final TransformingWriter root;
        private GenericEventWriterProvider provider;

        public WriterBuilder(UniversalEventWriter writer) {
            root = new TransformingWriter(writer, null);
            provider = new GenericEventWriterProvider();
        }

        public WriterBuilder morth(EventTransformer<Event, Event> transformer) {
            tailTransformers.add(transformer);
            return this;
        }

        public <T extends Event> WriterBuilder pass(Class<T> facade) {
            provider.add(facade, root);
            return this;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <T extends Event> WriterBuilder pass(Class<T> facade, EventTransformer<T, Event> transformer) {
            provider.add(facade, new TransformingWriter(root, (EventTransformer)transformer));
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T extends Event> WriterBuilder ignore(Class<T> facade) {
            return pass(facade, new VoidTransformer());
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <T extends TypedEventWriter> T facade(Class<T> facade) {
            if (!tailTransformers.isEmpty()) {
                EventTransformer[] chain = tailTransformers.toArray(new EventTransformer[0]);
                ChainTransformer tchain = new ChainTransformer(chain);
                root.transformer = tchain;
            }

            T writer = createWriter(facade, provider);
            provider = null;

            return writer;
        }
    }

    @SuppressWarnings("rawtypes")
    private static class VoidTransformer implements EventTransformer {
        @Override
        public Event transform(Event source) {
            return null;
        }
    }
}
