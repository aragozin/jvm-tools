package org.gridkit.jvmtool.jackson;

/**
 * Abstract base class that defines interface for customizing character
 * escaping aspects for String values, for formats that use escaping.
 * For JSON this applies to both property names and String values.
 *
 * @since 1.8
 */
public abstract class CharacterEscapes
{
    /**
     * Value used for lookup tables to indicate that matching characters
     * do not need to be escaped.
     */
    public final static int ESCAPE_NONE = 0;

    /**
     * Value used for lookup tables to indicate that matching characters
     * are to be escaped using standard escaping; for JSON this means
     * (for example) using "backslash - u" escape method.
     */
    public final static int ESCAPE_STANDARD = -1;

    /**
     * Value used for lookup tables to indicate that matching characters
     * will need custom escapes; and that another call
     * to {@link #getEscapeSequence} is needed to figure out exact escape
     * sequence to output.
     */
    public final static int ESCAPE_CUSTOM = -2;

    /**
     * Helper method that can be used to get a copy of standard JSON
     * escape definitions; this is useful when just wanting to slightly
     * customize definitions. Caller can modify this array as it sees
     * fit and usually returns modified instance via {@link #getEscapeCodesForAscii}
     */
    public static int[] standardAsciiEscapesForJSON()
    {
        int[] esc = CharTypes.get7BitOutputEscapes();
        int len = esc.length;
        int[] result = new int[len];
        System.arraycopy(esc, 0, result, 0, esc.length);
        return result;
    }
}
