package org.gridkit.jvmtool.stacktrace.analytics;

public class ParserException extends RuntimeException {

    private static final long serialVersionUID = 20151220L;

    private String parseText;
    private int offset;

    public ParserException(String parseText, int offset, String message) {
        super(message);
        this.parseText = parseText;
        this.offset = offset;
    }

    public ParserException(String parseText, int offset, String message, Exception e) {
        super(message, e);
        this.parseText = parseText;
        this.offset = offset;
    }

    public String getParseText() {
        return parseText;
    }

    public int getOffset() {
        return offset;
    }
}
