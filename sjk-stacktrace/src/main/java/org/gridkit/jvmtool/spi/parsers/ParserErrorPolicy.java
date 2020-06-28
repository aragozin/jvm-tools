package org.gridkit.jvmtool.spi.parsers;

public interface ParserErrorPolicy {

    enum Action {
        ABORT,
        CONTINUE
    }

    public Action onParserError(Exception e, String niceMessage);

    /**
     * Mixin interface for setting up {@link ParserErrorPolicy}
     *
     * @author Alexey Ragozin (alexey.ragozin@gmail.com)
     */
    public interface Aware {

        void setParserErrorPolicy(ParserErrorPolicy policy);
    }
}
