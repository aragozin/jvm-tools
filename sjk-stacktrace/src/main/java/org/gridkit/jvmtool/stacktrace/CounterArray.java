package org.gridkit.jvmtool.stacktrace;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CounterArray implements CounterCollection {

    public static CounterArray EMPTY = new CounterArray(new String[0], new long[0]) {

        @Override
        public void set(String key, long value) {
            throw new UnsupportedOperationException("Immutable");
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException("Immutable");
        }
    };

    String[] counterNames;
    long[] values;

    public CounterArray() {
        this(new String[0], new long[0]);
    }

    public CounterArray(String[] names, long[] values) {
        if (names.length != values.length) {
            throw new IllegalArgumentException("Array length mismatch");
        }
        this.counterNames = names;
        this.values = values;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            int n = 0;

            {seek();}

            protected void seek() {
                while(n < counterNames.length) {
                    if (counterNames[n] != null && values[n] != Long.MIN_VALUE) {
                        break;
                    }
                    ++n;
                }
            }

            @Override
            public boolean hasNext() {
                return n < counterNames.length;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                String key = counterNames[n];
                n++;
                seek();
                return key;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public long getValue(String key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        for(int i = 0; i != counterNames.length; ++i) {
            if (key.equals(counterNames[i])) {
                return values[i];
            }
        }
        return Long.MIN_VALUE;
    }

    public CounterArray clone() {
        int n = 0;
        for(@SuppressWarnings("unused") String key: this) {
            ++n;
        }
        String[] keys = new String[n];
        long[] vals = new long[n];
        n = 0;
        for(String key: this) {
            keys[n] = key;
            vals[n] = getValue(key);
            ++n;
        }
        return new CounterArray(keys, vals);
    }

    public void copyFrom(CounterCollection cc) {
        reset();
        for(String key: cc) {
            set(key, cc.getValue(key));
        }
    }

    public void set(String key, long value) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        for(int i = 0; i != counterNames.length; ++i) {
            if (key.equals(counterNames[i])) {
                values[i] = value;
                return;
            }
        }
        if (value != Long.MIN_VALUE) {
            int n = counterNames.length;
            counterNames = Arrays.copyOf(counterNames, n + 1);
            values = Arrays.copyOf(values, n + 1);
            counterNames[n] = key;
            values[n] = value;
        }
    }

    public void reset() {
        Arrays.fill(values, Long.MIN_VALUE);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(String key: this) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(key).append(": ").append(getValue(key));
        }
        sb.append(']');
        return sb.toString();
    }
}
