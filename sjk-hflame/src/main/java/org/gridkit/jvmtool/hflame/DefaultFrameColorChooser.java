/**
 * Copyright 2018 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.hflame;

import java.awt.Color;

import org.gridkit.jvmtool.stacktrace.StackFrame;

public class DefaultFrameColorChooser implements FrameColorChooser {

	private final int baseHue; 
	private final int deltaHue; 
	
	public DefaultFrameColorChooser() {
		baseHue = 12;
		deltaHue = 10;
	}
	
	@Override
	public int getFrameColor(String frame) {
		if (frame.startsWith("(")) {
			return -1;
		}
        int c = hashColor(baseHue, deltaHue, className(frame), methodName(frame));
		return c;
	}
	
	private String className(String frame) {
		int ch = frame.indexOf('(');
		if (ch >= 0) {
			frame = frame.substring(0, ch);
		}
		ch = frame.lastIndexOf('.');
		if (ch >= 0) {
			return frame.substring(0, ch);
		}
		else {
			return frame;
		}
	}

	private String methodName(String frame) {
		int ch = frame.indexOf('(');
		if (ch >= 0) {
			frame = frame.substring(0, ch);
		}
		ch = frame.lastIndexOf('.');
		if (ch >= 0) {
			return frame.substring(ch + 1);
		}
		else {
			return "";
		}
	}

	public static int hashColor(int baseHue, int deltaHue, String className, String methodName) {
        int hP = packageNameHash(className);
        int hC = classNameHash(className);
        int hM = methodName.hashCode();
        
        int hue = (0xFF) & (deltaHue == 0 ? baseHue : baseHue + (hP % (2 * deltaHue)) - deltaHue);
        int sat = 180 + (hC % 20) - 10;
        int lum = 220 + (hM % 20) - 10;
        
        int c = Color.HSBtoRGB(hue / 255f, sat / 255f, lum / 255f);
        return 0xFFFFFF & c;
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
