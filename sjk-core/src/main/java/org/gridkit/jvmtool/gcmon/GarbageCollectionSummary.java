package org.gridkit.jvmtool.gcmon;

public interface GarbageCollectionSummary {

    public long timestamp();

    public long duration();

    public String collectorName();

    public long collectionCount();

    public long collectionTotalTime();

    public Iterable<String> memorySpaces();

    public long memoryBefore(String spaceId);

    public long memoryAfter(String spaceId);

    public long memoryMax(String spaceId);

    public String memorySpaceName(String spaceId);

}
