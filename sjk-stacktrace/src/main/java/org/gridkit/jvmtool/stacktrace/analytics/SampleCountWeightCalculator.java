package org.gridkit.jvmtool.stacktrace.analytics;

import org.gridkit.jvmtool.event.CommonEvent;

class SampleCountWeightCalculator implements WeigthCalculator {

    @Override
    public long getWeigth(CommonEvent event) {
        return 1;
    }

    @Override
    public long getDenominator() {
        return 1;
    }

    @Override
    public String formatWeight(CaptionFlavour flavour, long weight) {
        switch (flavour) {
            default:
                return String.format("%d samples", weight);
        }
    }
}
