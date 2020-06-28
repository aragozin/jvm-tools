package org.gridkit.jvmtool.gcflow;

import java.util.Collection;

public interface GarbageCollectionSampler {

    public void report(String algoName, int eventsMissed, GcReport info);

    interface GcReport {

        public long getId();

        public boolean isYoungGC();

        public boolean isConcurrentGC();

        public long getCollectedSize();

        public long getPromotedSize();

        public long getTotalSizeBefore();

        public long getTotalSizeAfter();

        public Collection<String> getColletedPools();

        public Collection<String> getAllCollectedPools();

        public Collection<String> getAllMemoryPools();

        public long getSizeBefore(String pool);

        public long getSizeAfter(String pool);

        public long getSizeBefore(Collection<String> pools);

        public long getSizeAfter(Collection<String> pools);

        public long getTimeSincePreviousGC();

        public long getDuration();

        public long getJvmClockEndTime();

        public long getJvmClockStartTime();

        public long getWallClockEndTime();

        public long getWallClockStartTime();

        public Collection<String> getEdenPools();

        public Collection<String> getSurvivourPools();

        public Collection<String> getOldSpacePools();

        public Collection<String> getPermSpacePools();

    }
}
