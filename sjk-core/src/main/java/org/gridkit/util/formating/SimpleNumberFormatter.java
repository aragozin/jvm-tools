/**
 * Copyright 2014-2017 Alexey Ragozin
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
package org.gridkit.util.formating;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This is utility class capable for formating numbers as numbers and dates.
 * <p>
 * Internally it is using
 * <li> {@link SimpleDateFormat} - prefix T </li>
 * <li> {@link DecimalFormat} - prefix D </li>
 * <li> {@link String#format(String, Object...)} - prefix D </li>
 * <p>
 * Formatter always using neutral locale.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SimpleNumberFormatter implements NumberFormat {

    public static final SimpleNumberFormatter DEFAULT = new SimpleNumberFormatter("");
    
    private final LongFormatter lf;
    private final DoubleFormatter df;
    
    public SimpleNumberFormatter(String format) {
    	this(format, TimeZone.getDefault());
    }
    
    public SimpleNumberFormatter(String format, TimeZone tz) {
        if (format.length() == 0) {
            // default
            lf = new DefaultLongFormatter();
            df = new DefaultDoubleFormatter();
        }
        else if (format.startsWith("D")) {
            DecimalFormat fmt = new DecimalFormat(format.substring(1));
            lf = new DecimalFormatLong(fmt);
            df = new DecimalFormatDouble(fmt);
        }
        else if (format.startsWith("T")) {
            SimpleDateFormat sdf = new SimpleDateFormat(format.substring(1));
            sdf.setTimeZone(tz);
            lf = new SDFFormatter(sdf);
            df = new D2L(lf);
        }
        else if (format.startsWith("F")) {
            lf = new JavaFormatterLong(format.substring(1));
            df = new JavaFormatterDouble(format.substring(1));
        }
        else {
            throw new IllegalArgumentException("Format spec should start with D, T or F");
        }
    }
    
    public String formatLong(long v) {
        return lf.formatLong(v);
    }
    
    public String formatDouble(double v) {
        return df.formatDouble(v);
    }    
    
    private static abstract class LongFormatter {
        
        public abstract String formatLong(long v);
        
    }

    private static abstract class DoubleFormatter {
        
        public abstract String formatDouble(double v);
        
    }
    
    private static class DefaultLongFormatter extends LongFormatter {
        
        public String formatLong(long v) {
            return String.valueOf(v);
        }
    }
    
    private static class DefaultDoubleFormatter extends DoubleFormatter {
        
        public String formatDouble(double v) {
            return String.valueOf(v);
        }
    }
    
    private static class SDFFormatter extends LongFormatter {
        
        private final SimpleDateFormat sdf;

        public SDFFormatter(SimpleDateFormat sdf) {
            this.sdf = sdf;
        }

        @Override
        public String formatLong(long v) {
            return sdf.format(v);
        }
    }

    private static class DecimalFormatLong extends LongFormatter {
        
        private final java.text.DecimalFormat fmt;

        public DecimalFormatLong(DecimalFormat fmt) {
            this.fmt = fmt;
        }

        @Override
        public String formatLong(long v) {
            return fmt.format(v);
        }
    }

    private static class DecimalFormatDouble extends DoubleFormatter {
        
        private final java.text.DecimalFormat fmt;
        
        public DecimalFormatDouble(DecimalFormat fmt) {
            this.fmt = fmt;
        }
        
        @Override
        public String formatDouble(double v) {
            return fmt.format(v);
        }
    }
    
    private static class JavaFormatterLong extends LongFormatter {

        private final String fmt;
        
        public JavaFormatterLong(String fmt) {
            this.fmt = fmt;
        }

        @Override
        public String formatLong(long v) {
            return String.format(Locale.ROOT, fmt, v);
        }
    }

    private static class JavaFormatterDouble extends DoubleFormatter {
        
        private final String fmt;
        
        public JavaFormatterDouble(String fmt) {
            this.fmt = fmt;
        }

        @Override
        public String formatDouble(double v) {
            return String.format(Locale.ROOT, fmt, v);
        }        
    }
    
    private static class D2L extends DoubleFormatter {
        
        private final LongFormatter lf;

        public D2L(LongFormatter lf) {
            this.lf = lf;
        }

        @Override
        public String formatDouble(double v) {
            return lf.formatLong((long) v);
        }
    }
}
