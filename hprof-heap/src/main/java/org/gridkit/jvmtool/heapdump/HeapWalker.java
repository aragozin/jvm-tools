package org.gridkit.jvmtool.heapdump;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

public class HeapWalker {

    private static final Set<String> BOX_TYPES = new HashSet<String>();

    static {
        BOX_TYPES.add(Boolean.class.getName());
        BOX_TYPES.add(Byte.class.getName());
        BOX_TYPES.add(Short.class.getName());
        BOX_TYPES.add(Character.class.getName());
        BOX_TYPES.add(Integer.class.getName());
        BOX_TYPES.add(Long.class.getName());
        BOX_TYPES.add(Float.class.getName());
        BOX_TYPES.add(Double.class.getName());
    }

    private static final Map<String, InstanceConverter> CONVERTERS = new HashMap<String, InstanceConverter>();

    static {
        CONVERTERS.put(String.class.getName(), new InstanceConverter() {
            @Override
            public Object convert(Instance instance) {
                return stringValue(instance);
            }
        });
        InstanceConverter primitiveConverter = new InstanceConverter() {
            @Override
            public Object convert(Instance instance) {
                return primitiveValue(instance);
            }
        };
        for(String ptype: BOX_TYPES) {
            CONVERTERS.put(ptype, primitiveConverter);
        }
    }

    /**
     * Converts instances of few well know Java classes from
     * dump to normal java objects.
     * <br/>
     * Following are supported classes
     * <li>{@link String}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Byte}</li>
     * <li>{@link Short}</li>
     * <li>{@link Character}</li>
     * <li>{@link Integer}</li>
     * <li>{@link Long}</li>
     * <li>{@link Float}</li>
     * <li>{@link Double}</li>
     * <li>primitive arrays</li>
     * <li>object arrays</li>
     */
     /* TBD
     * <li>{@link Arrays#asList(Object...)}</li>
     * <li>{@link ArrayList} (subclasses would be reduced to {@link ArrayList})</li>
     * <li>{@link HashMap} (subclasses including {@link LinkedHashMap} would be reduced to {@link ArrayList})</li>
     * <li>{@link HashSet} (subclasses including {@link LinkedHashSet} would be reduced to {@link ArrayList})</li>
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(Instance obj) {
        if (obj == null) {
            return null;
        }
        JavaClass t = obj.getJavaClass();
        InstanceConverter c = CONVERTERS.get(obj.getJavaClass().getName());
        while(c == null && t.getSuperClass() != null) {
            t = t.getSuperClass();
            c = CONVERTERS.get(obj.getJavaClass().getName());
        }
        if (c == null) {
            // return instance as is
            return (T) obj;
        }
        else {
        return (T)c.convert(obj);
    }
    }

    /**
     * Converts value referenced for path from dump to normal java objects.
     * Few well known Java classes as long as primitive types are supported.
     * <br/>
     * Following are supported classes
     * <li>{@link String}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Byte}</li>
     * <li>{@link Short}</li>
     * <li>{@link Character}</li>
     * <li>{@link Integer}</li>
     * <li>{@link Long}</li>
     * <li>{@link Float}</li>
     * <li>{@link Double}</li>
     * <li>primitive arrays</li>
     * <li>object arrays</li>
     */
     /* TBD
     * <li>{@link Arrays#asList(Object...)}</li>
     * <li>{@link ArrayList} (subclasses would be reduced to {@link ArrayList})</li>
     * <li>{@link HashMap} (subclasses including {@link LinkedHashMap} would be reduced to {@link ArrayList})</li>
     * <li>{@link HashSet} (subclasses including {@link LinkedHashSet} would be reduced to {@link ArrayList})</li>
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(Instance obj, String pathSpec) {
        PathStep[] steps = HeapPath.parsePath(pathSpec, true);
        if (steps.length > 0 && steps[steps.length - 1] instanceof FieldStep) {
            PathStep[] shortPath = Arrays.copyOf(steps, steps.length - 1);
            FieldStep lastStep = (FieldStep) steps[steps.length - 1];
            String fieldName = lastStep.getFieldName();
            for(Instance i: HeapPath.collect(obj, shortPath)) {
                for(FieldValue fv: i.getFieldValues()) {
                    if ((fieldName == null && fv.getField().isStatic())
                            || (fieldName.equals(fv.getField().getName()))) {
                        if (fv instanceof ObjectFieldValue) {
                            return valueOf(((ObjectFieldValue) fv).getInstance());
                        }
                        else {
                            // have to use this as private package API is used behind scene
                            return (T) i.getValueOfField(fv.getField().getName());
                        }
                    }
                }
            }
            return null;
        }
        else {
            for(Instance i: HeapPath.collect(obj, steps)) {
                return valueOf(i);
            }
            return null;
        }
    }

    public static String stringValue(Instance obj) {
        if (obj == null) {
            return null;
        }
        if (!"java.lang.String".equals(obj.getJavaClass().getName())) {
            throw new IllegalArgumentException("Is not a string: " + obj.getInstanceId() + " (" + obj.getJavaClass().getName() + ")");
        }
        char[] text = null;
        int offset = 0;
        int len = -1;
        for(FieldValue f: obj.getFieldValues()) {
            Field ff = f.getField();
            if ("value".equals(ff.getName())) {
                PrimitiveArrayInstance chars = (PrimitiveArrayInstance) ((ObjectFieldValue)f).getInstance();
                text = new char[chars.getLength()];
                for(int i = 0; i != text.length; ++i) {
                    text[i] = ((String)chars.getValues().get(i)).charAt(0);
                }
            }
            // fields below were removed in Java 7
            else if ("offset".equals(ff.getName())) {
                offset = Integer.valueOf(f.getValue());
            }
            else if ("count".equals(ff.getName())) {
                len = Integer.valueOf(f.getValue());
            }
        }

        if (len < 0) {
            len = text.length;
        }

        return new String(text, offset, len);
    }

    @SuppressWarnings("unchecked")
    public static <T> T primitiveValue(Instance obj) {
        if (obj == null) {
            return null;
        }
        String className = obj.getJavaClass().getName();
        if (BOX_TYPES.contains(className)) {
            return (T)obj.getValueOfField("value");
        }
        else {
            throw new IllegalArgumentException("Is not a primitive wrapper: " + obj.getInstanceId() + " (" + obj.getJavaClass().getName() + ")");
        }
    }

    public static Iterable<Instance> walk(Instance root, String path) {
        return HeapPath.collect(root, HeapPath.parsePath(path, true));
    }

    public static Instance walkFirst(Instance root, String path) {
        Iterator<Instance> it = walk(root, path).iterator();
        if (it.hasNext()) {
            return it.next();
        }
        else {
            return null;
        }
    }

    private interface InstanceConverter {

        public Object convert(Instance instance);
    }
}
