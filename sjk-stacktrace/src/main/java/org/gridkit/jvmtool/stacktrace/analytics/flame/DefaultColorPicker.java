package org.gridkit.jvmtool.stacktrace.analytics.flame;

import java.awt.Color;

import org.gridkit.jvmtool.stacktrace.GenericStackElement;
import org.gridkit.jvmtool.stacktrace.StackFrame;

public class DefaultColorPicker implements FlameColorPicker {

    @Override
    public int pickColor(GenericStackElement[] trace) {

        if (trace.length == 0) {
            return 0xFFFFFF;
        }

        StackFrame sf = (StackFrame) trace[trace.length - 1];

        int c = hashColor(12, 10, sf);

        return c;
    }

    public static int hashColor(int baseHue, int deltaHue, StackFrame sf) {
        int hP = packageNameHash(sf.getClassName());
        int hC = classNameHash(sf.getClassName());
        int hM = sf.getMethodName().hashCode();

        int hue = deltaHue == 0 ? baseHue : baseHue + (hP % (2 * deltaHue)) - deltaHue;
        int sat = 180 + (hC % 20) - 10;
        int lum = 220 + (hM % 20) - 10;

        int c = Color.HSBtoRGB(hue / 255f, sat / 255f, lum / 255f);
        return c;
    }

    public static int hashGrayColor(StackFrame sf) {
        int hC = classNameHash(sf.getClassName());
        int hM = sf.getMethodName().hashCode();

        int hue = 0;
        int sat = 0;
        int lum = 220 + ((hM + hC) % 20) - 10;

        int c = Color.HSBtoRGB(hue / 255f, sat / 255f, lum / 255f);
        return c;
    }

    private static int packageNameHash(String className) {
        int c = className.lastIndexOf('.');
        if (c >= 0) {
            return className.substring(0, c).hashCode();
        }
        else {
            return 0;
        }
    }

    private static int classNameHash(String className) {
        int c = className.lastIndexOf('.');
        if (c >= 0) {
            className = className.substring(c + 1);
        }

        c = className.indexOf('$');
        if (c >= 0) {
            int nhash = className.substring(0, c).hashCode();
            int shash = className.substring(c + 1).hashCode();
            return nhash + (shash % 10);
        }
        else {
            return className.hashCode();
        }
    }
}
