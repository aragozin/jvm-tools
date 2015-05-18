package org.gridkit.jvmtool.stacktrace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BsvCodec {

    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final byte[] MAGIC1 = "BSV1".getBytes(UTF8);
    
    public static FlatSampleReader createReader(InputStream stream) throws IOException {
        byte[] mgc = new byte[4];
        int n = stream.read(mgc);
        if (n == 4 && Arrays.equals(MAGIC1, mgc)) {
            return new BsvReader(new GZIPInputStream(stream));
        }
        else {
            throw new IOException("Unsupported file format");
        }
    }

    public static FlatSampleWriter createWriter(OutputStream stream) throws IOException {
        stream.write(MAGIC1);
        GZIPOutputStream gzout = new GZIPOutputStream(stream);
        return new BsvWriter(gzout);
    }
    
    static final byte SVI_ZERO = 1; 
    static final byte SVI_MINUS_ONE = 2; 
    static final byte SVI_MIN32 = 3;
    static final byte SVI_MAX32 = 4;
    static final byte SVI_MIN64 = 5;
    static final byte SVI_MAX64 = 6;
    static final byte SVD_MIN = 8;
    static final byte SVD_ZERO = SVD_MIN + 0;
    static final byte SVD_NAN = SVD_MIN + 1;
    static final byte SVD_NINF = SVD_MIN + 2;
    static final byte SVD_PINF = SVD_MIN + 3;
    
    static final byte TAG_MDIC = 0;
    static final byte TAG_DDIC = 1;
    static final byte TAG_FRANGE = 2;
    static final byte TAG_FLIST = 3;
    static final byte TAG_SAMPLE = 4;

    static final byte WT_STRING = 0;
    static final byte WT_LONG = 1;
    static final byte WT_DOUBLE = 2;
    static final byte WT_SPEC = 3;

    static final long DRANGE_TARGET = 1l << 20;
    static final long DRANGE_NEG_THRESHOLD = -(1l << 44);
    
    private static class BsvReader implements FlatSampleReader {
        
        DataInputStream dis;
        
        List<String> permDic = new ArrayList<String>();
        List<String> rtrDic = new ArrayList<String>();
        List<SampleMeta> metaDic = new ArrayList<SampleMeta>();
        int dynamicBaseCounter = 0;
        long[] dynamicBase = new long[32];
        
        ValueMap tuple = new ValueMap();
        boolean eos = false;
        
        public BsvReader(InputStream is) {
            if (is instanceof DataInputStream) {
                dis = (DataInputStream)is;
            }
            else {
                dis = new DataInputStream(is);
            }
            permDic.add(null);
            rtrDic.add(null);
        }

        protected void ensureHasValue() {
            if (tuple.size == 0) {
                throw new NoSuchElementException("Reader should be primed");
            }
        }
        
        @Override
        public Collection<String> getAllFields() {
            ensureHasValue();
            return Arrays.asList(tuple.keys).subList(0, tuple.size);
        }

        @Override
        public boolean hasField(String field) {
            ensureHasValue();
            return tuple.hasField(field);
        }

        @Override
        public Object get(String field) {
            ensureHasValue();
            return tuple.getBoxed(field);
        }

        @Override
        public Class<?> getFieldType(String field) {
            ensureHasValue();
            return tuple.getType(field);
        }

        @Override
        public long getLong(String field) {
            ensureHasValue();
            return tuple.getLong(field);
        }

        @Override
        public double getDouble(String field) {
            ensureHasValue();
            return tuple.getDouble(field);
        }

        @Override
        public String getString(String field) {
            ensureHasValue();
            return tuple.getString(field);
        }

        @Override
        public void copyTo(FlatSampleWriter writer) {
            for(int i = 0; i != tuple.size; ++i) {
                String f = tuple.getFieldAt(i);
                if (tuple.type[i] == ValueMap.FSTRING) {
                    writer.set(f, tuple.svalues[i]);
                }
                else if (tuple.type[i] == ValueMap.FLONG) {
                    writer.set(f, tuple.lvalues[i]);
                }
                else if (tuple.type[i] == ValueMap.FDOUBLE) {
                    writer.set(f, tuple.dvalues[i]);
                }
            }
        }

        @Override
        public FlatSampleReader prime() {
            if (!hasValue()) {
                advance();
            }
            return this;
        }

        @Override
        public boolean hasValue() {
            return !eos && tuple.size > 0;
        }

        @Override
        public boolean advance() {
            try {
                tuple.clear();
                while(true) {                
                    int tag = dis.read();
                    if (tag < 0) {
                        eos = true;
                        return false;
                    }
                    switch(tag) {
                        case TAG_MDIC:
                            readMDicEntry();
                            break;
                        case TAG_DDIC:
                            readDDicEntry();
                            break;
                        case TAG_FLIST:
                            readFListEntry();
                            break;
                        case TAG_FRANGE:
                            readFrangeEntry();
                            break;
                        case TAG_SAMPLE:
                            readSample();
                            return true;
                        default:
                            throw new IOException("Unknown tag #" + tag);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void readMDicEntry() throws IOException {
            String str = dis.readUTF();
            permDic.add(str);
        }

        private void readDDicEntry() throws IOException {
            int n = readVarInt(dis) + 1;
            String str = dis.readUTF();
            if (rtrDic.size() > n) {
                rtrDic.set(n, str);
            }
            else {
                rtrDic.add(str);
            }
        }

        private void readFListEntry() throws IOException {
            int n = metaDic.size();
            int width = readVarInt(dis);
            SampleMeta sm = new SampleMeta();
            sm.reset(width);
            sm.id = n;
            sm.dynbaseId = new int[width];
            for(int i = 0; i != width; ++i) {
                int v = readVarInt(dis);
                sm.fields[i] = permDic.get(v >> 2);
                sm.wireTypes[i] = (byte) (3 & v);                 
            }
            for(int i = 0; i != width; ++i) {
                if (sm.wireTypes[i] == WT_LONG) {
                    sm.dynbaseId[i] = dynamicBaseCounter++;
                }
            }
            metaDic.add(sm);
        }

        private void readFrangeEntry() throws IOException {
            int dr = readVarInt(dis);
            long base = readVarLong(dis);
            setBase(dr, base);
        }

        private void readSample() throws IOException {
            int meta = readVarInt(dis);
            SampleMeta sm = metaDic.get(meta);
            for(int i = 0; i != sm.width; ++i) {
                String f = sm.fields[i];
                if (sm.wireTypes[i] == WT_SPEC) {
                    int n = readVarInt(dis);
                    switch(n) {
                        case SVI_ZERO: tuple.put(f, 0); break;
                        case SVI_MINUS_ONE: tuple.put(f, -1); break;
                        case SVI_MIN32: tuple.put(f, Integer.MIN_VALUE); break;
                        case SVI_MAX32: tuple.put(f, Integer.MAX_VALUE); break;
                        case SVI_MIN64: tuple.put(f, Long.MIN_VALUE); break;
                        case SVI_MAX64: tuple.put(f, Long.MAX_VALUE); break;
                        case SVD_ZERO: tuple.put(f, 0d); break;
                        case SVD_NAN: tuple.put(f, Double.NaN); break;
                        case SVD_NINF: tuple.put(f, Double.NEGATIVE_INFINITY); break;
                        case SVD_PINF: tuple.put(f, Double.POSITIVE_INFINITY); break;
                        default:
                            throw new IOException("Unknown special value #" + n);
                    }
                }
                else if (sm.wireTypes[i] == WT_STRING) {
                    int n = readVarInt(dis);
                    tuple.put(f, rtrDic.get(n));
                }
                else if (sm.wireTypes[i] == WT_LONG) {
                    long n = readVarLong(dis);
                    long base = getBase(sm.dynbaseId[i]);
                    tuple.put(f, base + n);
                }
                else if (sm.wireTypes[i] == WT_DOUBLE) {
                    double n = dis.readDouble();
                    tuple.put(f, n);
                }
            }
        }
        
        private long getBase(int dr) {
            return dr >= dynamicBase.length ? 0 : dynamicBase[dr];
        }

        private void setBase(int dr, long value) {
            if (dr >= dynamicBase.length) {
                dynamicBase = Arrays.copyOf(dynamicBase, Math.max(2 * dynamicBase.length, dr + 1));
            }
            dynamicBase[dr] = value;
        }        
    }
    
    private static class BsvWriter implements FlatSampleWriter {

        private DataOutputStream dos;
        
        private Map<String, Integer> permDic = new HashMap<String, Integer>();
        private RotatingStringDictionary rtrDic = new RotatingStringDictionary(20 << 10);
        private Map<SampleMeta, SampleMeta> metaDic = new HashMap<SampleMeta, SampleMeta>();        
        
        private int dynamicBaseCounter = 0;
        private long[] dynamicBase = new long[32];
        
        private SampleMeta metaLookup = new SampleMeta();
        private SampleMeta tupleMeta;
        private ValueMap tuple = new ValueMap();
        private int[] stringBuf = new int[32];

        public BsvWriter(OutputStream os) {
            if (os instanceof DataOutputStream) {
                dos = (DataOutputStream) os;
            }
            else {
                dos = new DataOutputStream(os);
            }
        }
        
        @Override
        public FlatSampleWriter set(String field, long value) {
            tuple.put(field, value);
            return this;
        }

        @Override
        public FlatSampleWriter set(String field, double value) {
            tuple.put(field, value);
            return this;
        }

        @Override
        public FlatSampleWriter set(String field, String value) {
            tuple.put(field, value); 
            return this;
        }

        @Override
        public void push() throws IOException {
            if (tuple.size == 0) {
                throw new IllegalArgumentException("Tuple has not fields");
            }
            ensureSampleMeta();
            adjustDynBase();
            writeSample();
            tuple.clear();
            tupleMeta = null;
        }

        private void writeSample() throws IOException {
            if (tupleMeta.width > stringBuf.length) {
                stringBuf = new int[tupleMeta.width];
            }
            
            for(int i = 0; i != tupleMeta.width; ++i) {
                if (tupleMeta.wireTypes[i] == WT_STRING) {
                    stringBuf[i] = ensureRtrString(tuple.svalues[i]);
                }
            }
            
            dos.writeByte(TAG_SAMPLE);
            writeVarInt(dos, tupleMeta.id);
            for (int i = 0; i != tupleMeta.width; ++i) {
                switch(tupleMeta.wireTypes[i]) {
                    case WT_STRING: 
                        writeVarInt(dos, stringBuf[i]);
                        break;
                    case WT_DOUBLE:
                        dos.writeDouble(tuple.dvalues[i]);
                        break;
                    case WT_LONG:
                        long base = getBase(tupleMeta.dynbaseId[i]);
                        writeVarLong(dos, tuple.lvalues[i] - base);
                        break;
                    case WT_SPEC:
                        writeVarInt(dos, tuple.getSvCodeAt(i));
                        break;
                    default:
                        throw new RuntimeException("Unreachable");
                }
            }
        }

        private void adjustDynBase() throws IOException {
            for(int i = 0; i != tupleMeta.width; ++i) {
                if (tupleMeta.wireTypes[i] == WT_LONG) {
                    int dr = tupleMeta.dynbaseId[i];
                    long l = tuple.lvalues[i];
                    adjustDynBase(dr, l);
                }
            }
        }

        private void adjustDynBase(int dr, long l) throws IOException {
            long base = getBase(dr);
            long m = l - base;
            if (m < 0 && m > DRANGE_NEG_THRESHOLD) {
                // adjust offset
                base = l;
                setBase(dr, base);
                dos.writeByte(TAG_FRANGE);
                writeVarInt(dos, dr);
                writeVarLong(dos, base);
            }
            else if (m > DRANGE_TARGET && base == 0) {
                // init offset
                base = l;
                setBase(dr, base);
                dos.writeByte(TAG_FRANGE);
                writeVarInt(dos, dr);
                writeVarLong(dos, base);                
            }
        }

        private long getBase(int dr) {
            return dr >= dynamicBase.length ? 0 : dynamicBase[dr];
        }

        private void setBase(int dr, long value) {
            if (dr >= dynamicBase.length) {
                dynamicBase = Arrays.copyOf(dynamicBase, Math.max(2 * dynamicBase.length, dr + 1));
            }
            dynamicBase[dr] = value;
        }

        private int ensureSampleMeta() throws IOException {
            metaLookup.reset(tuple.size);
            for(int i = 0; i != tuple.size; ++i) {
                metaLookup.fields[i] = tuple.getFieldAt(i);
                metaLookup.wireTypes[i] = tuple.getWireTypeAt(i);
            }
            
            SampleMeta sm = metaDic.get(metaLookup);
            if (sm == null) {
                sm = new SampleMeta();
                sm.id = metaDic.size();
                sm.width = metaLookup.width;
                sm.fields = Arrays.copyOf(metaLookup.fields, sm.width);
                sm.wireTypes = Arrays.copyOf(metaLookup.wireTypes, sm.width);
                sm.dynbaseId = new int[sm.width];
                for(int i = 0; i != sm.width; ++i) {
                    if (sm.wireTypes[i] == WT_LONG) {
                        sm.dynbaseId[i] = dynamicBaseCounter++;
                    }
                }
                for(int i = 0; i != sm.width; ++i) {
                    ensurePermString(sm.fields[i]);
                }
                
                metaDic.put(sm, sm);
                
                dos.writeByte(TAG_FLIST);
                writeVarInt(dos, sm.width);
                for(int i = 0; i != sm.width; ++i) {
                    writeVarInt(dos, ensurePermString(sm.fields[i]) << 2 | sm.wireTypes[i]);
                }
            }
            
            tupleMeta = sm;
            
            return tupleMeta.id;
        }

        private int ensurePermString(String str) throws IOException {
            if (str == null) {
                return 0;
            }
            Integer n = permDic.get(str);
            if (n == null) {
                n = permDic.size() + 1;
                permDic.put(str, n);
                dos.writeByte(TAG_MDIC);
                dos.writeUTF(str);
            }
            return n;
        }

        private int ensureRtrString(String str) throws IOException {
            if (str == null) {
                return 0;
            }
            int n = rtrDic.intern(str);
            if (n < 0) {
                n = ~n;
                dos.writeByte(TAG_DDIC);
                writeVarInt(dos, n + 1);
                dos.writeUTF(str);
            }
            return n + 1;
        }

        @Override
        public void close() throws IOException {            
            dos.close();
        }
    }
    
    private static class SampleMeta {
        
        private int id;
        private int width;
        private String[] fields = new String[16];
        private byte[] wireTypes = new byte[16];
        private int[] dynbaseId;
        private int hash = 0;

        void reset(int width) {
            hash = 0;
            this.width = width;
            if (fields.length < width) {
                fields = new String[width];
                wireTypes = new byte[width];
            }
            id = 0;
            dynbaseId = null;
        }
        
        public int hashCode() {
            if (hash == 0) {
                int h = 31;
                for(int i = 0; i != width; ++i) {
                    h ^= fields[i].hashCode() << wireTypes[i];
                    h <<= 3;
                }
                if (h == 0) {
                    h = 1;
                }
                hash = h;
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            else if (obj.getClass() != getClass()) {
                return false;
            }
            else {
                SampleMeta that = (SampleMeta) obj;
                if (width != that.width) {
                    return false;
                }
                for(int i = 0; i != width; ++i) {
                    if (!fields[i].equals(that.fields[i]) || wireTypes[i] != that.wireTypes[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
    }
    
    private static class ValueMap {

        static final byte FSTRING = 0;
        static final byte FLONG = 1;
        static final byte FDOUBLE = 2;
        
        String[] keys = new String[16];
        byte[] type = new byte[16];
        String[] svalues = new String[16];
        long[] lvalues = new long[16];
        double[] dvalues = new double[16];
        int size = 0;

        public void clear() {
            size = 0;
            Arrays.fill(keys, null);
            Arrays.fill(svalues, null);
        }
        
        public String getFieldAt(int n) {
            return keys[n];
        }

        public boolean hasField(String field) {
            return Arrays.binarySearch(keys, 0, size, field) >= 0;
        }

        public byte getWireTypeAt(int n) {
            if (type[n] == FSTRING) {
                return WT_STRING;
            }
            else if (type[n] == FLONG) {
                if (lvalues[n] == 0 
                  || lvalues[n] == -1
                  || lvalues[n] == Integer.MIN_VALUE
                  || lvalues[n] == Integer.MAX_VALUE
                  || lvalues[n] == Long.MIN_VALUE
                  || lvalues[n] == Long.MAX_VALUE
                  ) {
                    return WT_SPEC;
                }
                else {
                    return WT_LONG;
                }
            }
            else /* if (type[n] == FDOUBLE) */ {
                if (dvalues[n] == 0 
                 || Double.isNaN(dvalues[n])
                 || Double.isInfinite(dvalues[n])
                 ) {
                    return WT_SPEC;
                }
                else {
                    return WT_DOUBLE;
                }
            }
        }

        public int getSvCodeAt(int n) {
            if (type[n] == FLONG) {
                if (lvalues[n] == 0) {
                    return SVI_ZERO;
                }
                if (lvalues[n] == -1) {
                    return SVI_MINUS_ONE;
                }
                if (lvalues[n] == Integer.MIN_VALUE) {
                    return SVI_MIN32;
                }
                if (lvalues[n] == Integer.MAX_VALUE) {
                    return SVI_MAX32;
                }
                if (lvalues[n] == Long.MIN_VALUE) {
                    return SVI_MIN64;
                }
                if (lvalues[n] == Long.MAX_VALUE) {
                    return SVI_MAX64;
                }
                throw new RuntimeException("Unreachable");
            }
            else /* if (type[n] == FDOUBLE) */ {
                if (dvalues[n] == 0) {
                    return SVD_ZERO;
                }
                if (Double.isNaN(dvalues[n])) {
                    return SVD_NAN;
                }
                if (dvalues[n] == Double.NEGATIVE_INFINITY) {
                    return SVD_NINF;
                }
                if (dvalues[n] == Double.POSITIVE_INFINITY) {
                    return SVD_PINF;
                }
                throw new RuntimeException("Unreachable");
            }
        }

        public Object getBoxed(String field) {
            int n = Arrays.binarySearch(keys, 0, size, field);
            if (n < 0) {
                return null;
            }
            switch(type[n]) {
                case FSTRING: return svalues[n];
                case FLONG: return lvalues[n];
                case FDOUBLE: return dvalues[n];
                default: return null;
            }
        }

        public Class<?> getType(String field) {
            int n = Arrays.binarySearch(keys, 0, size, field);
            if (n < 0) {
                return null;
            }
            switch(type[n]) {
                case FSTRING: return String.class;
                case FLONG: return long.class;
                case FDOUBLE: return double.class;
                default: return null;
            }
        }

        public long getLong(String field) {
            int n = Arrays.binarySearch(keys, 0, size, field);
            if (n < 0 || type[n] != FLONG) {
                throw new IllegalArgumentException("Not a long field '" + field + "'");
            }
            else {
                return lvalues[n];                        
            }
        }

        public double getDouble(String field) {
            int n = Arrays.binarySearch(keys, 0, size, field);
            if (n < 0 || type[n] != FDOUBLE) {
                throw new IllegalArgumentException("Not a long field '" + field + "'");
            }
            else {
                return dvalues[n];                        
            }
        }

        public String getString(String field) {
            int n = Arrays.binarySearch(keys, 0, size, field);
            if (n < 0 || type[n] != FSTRING) {
                throw new IllegalArgumentException("Not a long field '" + field + "'");
            }
            else {
                return svalues[n];                        
            }
        }
        
        public void put(String field, String value) {
            int n = putKey(field);
            putString(n, value);
        }

        public void put(String field, long value) {
            int n = putKey(field);
            putLong(n, value);
        }

        public void put(String field, double value) {
            int n = putKey(field);
            putDouble(n, value);
        }

        private int putKey(String field) {
            int n = Arrays.binarySearch(keys, 0, size, field);
            if (n >= 0) {
                return n;
            }
            else {
                ++size; 
                n = ~n;
                if (size > keys.length) {
                    keys = Arrays.copyOf(keys, 2 * keys.length);
                }
                for(int i = size - 1; i != n; --i) {
                    keys[i] = keys[i - 1];
                }
                keys[n] = field;
                return n;
            }
        }
        
        private void putString(int n, String value) {
            if (svalues.length <= n) {
                svalues = Arrays.copyOf(svalues, Math.max(n + 1, 2 * svalues.length));
            }
            setType(n, FSTRING);
            svalues[n] = value;
        }
        
        private void putLong(int n, long value) {
            if (lvalues.length <= n) {
                lvalues = Arrays.copyOf(lvalues, Math.max(n + 1, 2 * lvalues.length));
            }
            setType(n, FLONG);
            lvalues[n] = value;
        }
        
        private void putDouble(int n, double value) {
            if (dvalues.length <= n) {
                dvalues = Arrays.copyOf(dvalues, Math.max(n + 1, 2 * dvalues.length));
            }
            setType(n, FDOUBLE);
            dvalues[n] = value;
        }

        private void setType(int n, int ftype) {
            if (type.length <= n) {
                type = Arrays.copyOf(type, Math.max(n + 1, 2 * type.length));
            }
            type[n] = (byte) ftype;
        }
    }
    
    static int readVarInt(DataInputStream dis) throws IOException {
        int b = dis.readByte();
        if ((b & 0x80) == 0) {
            return 0x7F & b;
        }
        else {
            int v = (0x7F & b);
            b = dis.readByte();
            v |= ((0x7F & b) << 7);
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7F & b) << 14);
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0xFF & b) << 21);
            return v;
        }
    }

    static long readVarLong(DataInputStream dis) throws IOException {
        byte b = dis.readByte();
        if ((b & 0x80) == 0) {
            return 0x7F & b;
        }
        else {
            long v = (0x7F & b);
            b = dis.readByte();
            v |= ((0x7Fl & b) << 7); // byte 2
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 14); // byte 3
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 21); // byte 4
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 28); // byte 5
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 35); // byte 6
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 42); // byte 7
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0x7Fl & b) << 49); // byte 8
            if ((b & 0x80) == 0) {
                return v;
            }
            b = dis.readByte();
            v |= ((0xFFl & b) << 56); // byte 9

            return v;
        }
    }

    static void writeVarInt(DataOutputStream dos, int v) throws IOException {
        if (v < 0) {
            throw new IllegalArgumentException("Out of bounds: " + v);
        }
        int val = v;
        if ((val & 0xFFFFFF80) == 0) {
            dos.write(val);
            return;
        }
        dos.write(0x80 | (0x7F & val));
        val >>= 7;
        if ((val & 0xFFFFFF80) == 0) {
            dos.write(val);
            return;
        }
        dos.write(0x80 | (0x7F & val));
        val >>= 7;
        if ((val & 0xFFFFFF80) == 0) {
            dos.write(val);
            return;
        }
        dos.write(0x80 | (0x7F & val));
        val >>= 7;
        if ((val & 0xFFFFFF00) == 0) {
            dos.write(val);
            return;
        }
        else {
            throw new IllegalArgumentException("Out of bounds: " + v);
        }
    }

    static void writeVarLong(DataOutputStream dos, long v) throws IOException {
        long val = v;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) {
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 2
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 3
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 4
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 5
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 6
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 7
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF80l) == 0) { // byte 8
            dos.write((int)(0xFF & val));
            return;
        }
        dos.write(0x80 | (int)(0x7F & val));
        val >>>= 7;
        if ((val & 0xFFFFFFFFFFFFFF00l) == 0) { // byte 9
            dos.write((int)(0xFF & val));
            return;
        }
        else {
            throw new IllegalArgumentException("Out of bounds: " + v);
        }
    }
}
