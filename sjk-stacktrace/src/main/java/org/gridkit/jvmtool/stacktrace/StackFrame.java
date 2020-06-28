package org.gridkit.jvmtool.stacktrace;

import java.util.Comparator;

public class StackFrame implements CharSequence, GenericStackElement {

    public static final Comparator<StackFrame> COMPARATOR = new FrameComparator();

    private static final String NATIVE_METHOD = "Native Method";
    private static final String UNKNOWN_SOURCE = "Unknown Source";

    private static final int NO_LINE_NUMBER = 0;
    private static final int NO_SOURCE = -1;
    private static final int NATIVE = -2;

    private static final int[] PO10 = {1, 10, 100, 1000, 10000, 100000};

    private final String classPrefix;
    private final String className;
    private String fqcn = null; // fully qualified name
    private final String methodName;
    private final String fileName;
    private final int lineNumber;

    private final short textLen;
    private final short lineNumberDigits;

    private final int hash;

    public StackFrame(StackTraceElement ste) {
        this(null, ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber());
    }

    public StackFrame(String classPrefix, String className, String methodName, String fileName, int lineNumber) {
        this.classPrefix = "".equals(classPrefix) ? null : classPrefix;
        if (className == null) {
            throw new NullPointerException("Class name cannot be null");
        }
        this.className = className;
        this.methodName = methodName;
        this.fileName = lineNumber == -2 ? null : fileName; // fileName is ignored for native
        this.lineNumber = lineNumber;
        if (lineNumber == -2) {
            lineNumberDigits = NATIVE;
        }
        else if (fileName == null) {
            lineNumberDigits = NO_SOURCE;
        }
        else if (lineNumber == -1) {
            lineNumberDigits = NO_LINE_NUMBER;
        }
        else {
            lineNumberDigits = (short) (
                    lineNumber < 10 ? 1 :
                    lineNumber < 100 ? 2 :
                    lineNumber < 1000 ? 3 :
                    lineNumber < 10000 ? 4 :
                    lineNumber < 100000 ? 5 :
                    lineNumber < 1000000 ? 6 :
                    String.valueOf(lineNumber).length()
            );
        }
        int len = calcLen();
        if (len > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Frame is too big");
        }
        else {
            textLen = (short) len;
        }
        hash = genHash();
    }

    private int calcLen() {
        int len = classPrefix == null ? 0 : (classPrefix.length() + 1);
        len += className.length();
        len += methodName.length() + 1;
        len += 1; // (
        switch(lineNumberDigits) {
            case NO_LINE_NUMBER: len += fileName.length();
            break;
            case NO_SOURCE: len += UNKNOWN_SOURCE.length();
            break;
            case NATIVE: len += NATIVE_METHOD.length();
            break;
            default:
                len += fileName.length() + 1 + lineNumberDigits;
        }
        len += 1; // )
        return len;
    }

    public String getClassName() {
        if (fqcn == null) {
            fqcn = classPrefix == null ? className : (classPrefix + "." + className);
        }
        return fqcn;
    }

    String getClassPrefix() {
        return classPrefix;
    }

    String getShortClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSourceFile() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isNative() {
        return lineNumber == NATIVE;
    }

    public StackFrame internSymbols() {
        String cp = classPrefix == null ? null : classPrefix.intern();
        String cn = className.intern();
        String mn = methodName.intern();
        String fn = fileName == null ? null : fileName.intern();

        if (cp != classPrefix || cn != className || mn != methodName || fn != fileName) {
            return new StackFrame(cp, cn, mn, fn, lineNumber);
        }
        else {
            return this;
        }
    }

    @Override
    public int length() {
        return textLen;
    }

    @Override
    public char charAt(int index) {
        if (index > (textLen - 1)) {
            throw new IndexOutOfBoundsException();
        }
        else if (index == (textLen - 1)) {
            return ')';
        }

        int pref = classPrefix == null ? 0 : (classPrefix.length());
        if (pref > 0) {
            if (index < pref) {
                return classPrefix.charAt(index);
            }
            else if (index == pref) {
                return '.';
            }
            pref += 1;
        }
        int cn = pref + className.length();
        if (index < cn) {
            return className.charAt(index - pref);
        }
        else if (index == cn) {
            return '.';
        }
        cn += 1;
        int mn = cn + methodName.length();

        if (index < mn) {
            return methodName.charAt(index - cn);
        }
        else if (index == mn) {
            return '(';
        }

        mn += 1;

        switch(lineNumberDigits) {
            case NO_LINE_NUMBER:
                return fileName.charAt(index - mn);
            case NO_SOURCE:
                try {
                    return UNKNOWN_SOURCE.charAt(index - mn);
                }
                catch ( StringIndexOutOfBoundsException e) {
                    throw e;
                }
            case NATIVE:
                return NATIVE_METHOD.charAt(index - mn);
            default:
                int fn = mn + fileName.length();
                if (index < fn) {
                    return fileName.charAt(index - mn);
                }
                else if (index == fn) {
                    return ':';
                }
                int d = lineNumberDigits - (index - fn);
                int p = PO10[d];
                return (char)('0' + ((lineNumber / p) % 10));
        }
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start > textLen || end > textLen) {
            throw new IndexOutOfBoundsException();
        }
        if (start > end) {
            throw new IllegalArgumentException();
        }
        return new Subsequence(this, start, end - start);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StackFrame other = (StackFrame) obj;
        if (textLen != other.textLen || hash != other.hash) {
            return false;
        }
        if (fileName == null) {
            if (other.fileName != null)
                return false;
        } else if (!fileName.equals(other.fileName))
            return false;
        if (lineNumber != other.lineNumber)
            return false;
        if (methodName == null) {
            if (other.methodName != null)
                return false;
        } else if (!methodName.equals(other.methodName)) {
            return false;
        }
        if (classPrefix != null && other.classPrefix != null) {
            if (!classPrefix.equals(other.classPrefix)) {
                return false;
            }
            if (!className.equals(other.className)) {
                return false;
            }
        }
        else if (classPrefix == null && other.classPrefix == null) {
            if (!className.equals(other.className)) {
                return false;
            }
        }
        else {
            if (classPrefix == null) {
                if (!className.startsWith(other.classPrefix) || !className.endsWith(other.className) || className.charAt(other.classPrefix.length()) != '.') {
                    return false;
                }
            }
            else {
                if (!other.className.startsWith(classPrefix) || !other.className.endsWith(className) || other.className.charAt(classPrefix.length()) != '.') {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @return stack frame instance with removed source information
     */
    public StackFrame withoutSource() {
        return new StackFrame(classPrefix, className, methodName, null, -1);
    }

    public StackTraceElement toStackTraceElement() {
        String cn = classPrefix == null ? className : (classPrefix + '.' + className);
        return new StackTraceElement(cn, methodName, fileName, lineNumber);
    }

    @Override
    public String toString() {
        return toString(this);
    }

    public void toString(StringBuilder builder) {
        if (classPrefix != null) {
            builder.append(classPrefix).append('.');
        }
        builder.append(className).append('.');
        builder.append(methodName).append('(');
        switch(lineNumberDigits) {
            case NO_LINE_NUMBER:
                builder.append(fileName);
                break;
            case NO_SOURCE:
                builder.append(UNKNOWN_SOURCE);
                break;
            case NATIVE:
                builder.append(NATIVE_METHOD);
                break;
            default:
                builder.append(fileName).append(':').append(lineNumber);
        }
        builder.append(')');
    }

    private int genHash() {
        int hash = 1033;
        if (classPrefix != null) {
            hash = hashText(hash, classPrefix);
            hash = 31 * hash + '.';
        }
        hash = hashText(hash, className);
        hash = 31 * hash + '.';
        hash = hashText(hash, methodName);
        switch(lineNumberDigits) {
            case NO_LINE_NUMBER:
                hash = hashText(hash, fileName);
                break;
            case NO_SOURCE:
                hash = 31 * hash + -1;
                break;
            case NATIVE:
                hash = 31 * hash + -2;
                break;
            default:
                hash = hashText(hash, fileName);
                hash = 31 * hash + lineNumber;
        }
        return hash;
    }

    private int hashText(int hash, String text) {
        for(int i = 0; i != text.length(); ++i) {
            hash = 31 * hash + text.charAt(i);
        }
        return hash;
    }

    private static String toString(CharSequence seq) {
        char[] buf = new char[seq.length()];
        for(int i = 0; i != buf.length; ++i) {
            buf[i] = seq.charAt(i);
        }
        return new String(buf);
    }

    public static StackFrame parseFrame(String line) {
        StringBuilder sb = new StringBuilder(line.length());
        int dot1 = -1;
        int dot2 = -1;
        int n = 0;
        while(true) {
            char ch = line.charAt(n);
            if (ch == '(') {
                break;
            }
            if (ch == '.') {
                dot2 = dot1;
                dot1 = n;
            }
            sb.append(ch);
            ++n;
            if (n >= line.length()) {
                throw new IllegalArgumentException("Cannot parse [" + line + "]");
            }
        }
        if (dot1 == -1) {
            throw new IllegalArgumentException("Cannot parse [" + line + "]");
        }
        String pref = null;
        String cn = null;
        String mn = null;
        if (dot2 != -1) {
            pref = sb.substring(0, dot2);
            cn = sb.substring(dot2 + 1, dot1);
            mn = sb.substring(dot1 + 1);
        }
        else {
            cn = sb.substring(0, dot1);
            mn = sb.substring(dot1 + 1);
        }
        sb.setLength(0);
        int col = -1;
        ++n;
        int off = n;
        while(true) {
            char ch = line.charAt(n);
            if (ch == ')') {
                break;
            }
            if (ch == ':') {
                col = n - off;
            }
            sb.append(ch);
            ++n;
            if (n >= line.length()) {
                throw new IllegalArgumentException("Cannot parse [" + line + "]");
            }
        }
        String file = null;
        int lnum = -1;
        if (col != -1) {
            file = sb.substring(0, col);
            try {
                lnum = Integer.parseInt(sb.substring(col + 1));
            }
            catch(NumberFormatException e) {
                throw new IllegalArgumentException("Number format exception '" + e.getMessage() + "' parsing [" + line + "]");
            }
        }
        else {
            file = sb.toString();
            if (file.equals(NATIVE_METHOD)) {
                file = null;
                lnum = -2;
            }
            else if (file.equals(UNKNOWN_SOURCE)) {
                file = null;
            }
        }
        return new StackFrame(pref, cn, mn, file, lnum);
    }

    private static class Subsequence implements CharSequence {

        private CharSequence seq;
        private int offs;
        private int len;

        public Subsequence(CharSequence seq, int offs, int len) {
            this.seq = seq;
            this.offs = offs;
            this.len = len;
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public char charAt(int index) {
            if (index >= len) {
                throw new IndexOutOfBoundsException();
            }
            return seq.charAt(offs + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start > len || end > len) {
                throw new IndexOutOfBoundsException();
            }
            if (start > end) {
                throw new IllegalArgumentException();
            }
            return new Subsequence(seq, offs + start, end - start);
        }

        @Override
        public String toString() {
            return StackFrame.toString(this);
        }
    }

    private static class FrameComparator implements Comparator<StackFrame> {

        @Override
        public int compare(StackFrame o1, StackFrame o2) {
            int n = compare(o1.getClassName(), o2.getClassName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getLineNumber(), o2.getLineNumber());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getMethodName(), o2.getMethodName());
            if (n != 0) {
                return n;
            }
            n = compare(o1.getSourceFile(), o2.getSourceFile());
            return 0;
        }

        private int compare(int n1, int n2) {
            return Long.signum(((long)n1) - ((long)n2));
        }

        private int compare(String str1, String str2) {
            if (str1 == str2) {
                return 0;
            }
            else if (str1 == null) {
                return -1;
            }
            else if (str2 == null) {
                return 1;
            }
            return str1.compareTo(str2);
        }
    }
}
