package org.gridkit.jvmtool.event;

public interface TagCollection extends Iterable<String> {

    public static TagCollection EMPTY = new EmptyTagCollection();

    public Iterable<String> tagsFor(String key);

    public String firstTagFor(String key);

    public boolean contains(String key, String tag);

    public TagCollection clone();

}
