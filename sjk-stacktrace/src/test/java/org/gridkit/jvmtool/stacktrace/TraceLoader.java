package org.gridkit.jvmtool.stacktrace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class TraceLoader {

    public static StackFrame[] loadFrames(String res) {
        return loadFrames(res);
    }

    public static StackFrame[] loadFrames(String res, int multiplier) {
        try {
            List<StackFrame> frames = new ArrayList<StackFrame>();
            for(int i = 0; i != multiplier; ++i) {
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while(true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.trim().length() > 0)
                    frames.add(StackFrame.parseFrame(line.trim()));
                }
            }
            return frames.toArray(new StackFrame[frames.size()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    public static StackFrame[] parseFrames(String trace) {
        try {
            List<StackFrame> frames = new ArrayList<StackFrame>();
            BufferedReader br = new BufferedReader(new StringReader(trace));
            while(true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.trim().length() > 0)
                    frames.add(StackFrame.parseFrame(line.trim()));
            }
            return frames.toArray(new StackFrame[frames.size()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };
}
