package org.gridkit.jvmtool;

import java.text.DecimalFormat;

import org.gridkit.jvmtool.event.CommonEvent;
import org.gridkit.jvmtool.stacktrace.analytics.WeigthCalculator;

/**
 * ViusalVM stores sampling data as a path tree weighted by
 * milliseconds.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class VisualVMSampleCalculator implements WeigthCalculator {

    public static final String NODE_WEIGHT = "netbeans.sampleWeight";

    @Override
    public long getWeigth(CommonEvent event) {
        return event.counters().getValue(NODE_WEIGHT);
    }

    @Override
    public long getDenominator() {
        return 1000;
    }

    @Override
    public String formatWeight(CaptionFlavour flavour, long weight) {
        DecimalFormat df = new DecimalFormat("#.##");
        switch (flavour) {
            default:
                return df.format(weight / 1000f) + "ms";
        }
    }
}
