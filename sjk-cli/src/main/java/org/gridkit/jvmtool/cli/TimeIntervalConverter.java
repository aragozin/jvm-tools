/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.jvmtool.cli;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.IStringConverter;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class TimeIntervalConverter implements IStringConverter<Long> {

    private static int DURATION_GROUP = 1;
    private static int TIME_UNIT_GROUP = 2;

    private static final String TI_REGEX = "(\\d+)\\s*(\\w+)";
    private static final Pattern TI_PATTERN = Pattern.compile(TI_REGEX);

    private static final String MTI_REGEX = String.format("(%s\\s+)+", TI_REGEX);
    private static final Pattern MTI_PATTERN = Pattern.compile(MTI_REGEX);

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private static final Map<String, TimeUnit> timeUnitAlias = new HashMap<String, TimeUnit>();

    static {
        timeUnitAlias.put("ms", TimeUnit.MILLISECONDS);
        timeUnitAlias.put("s",  TimeUnit.SECONDS);
        timeUnitAlias.put("m",  TimeUnit.MINUTES);
        timeUnitAlias.put("h",  TimeUnit.HOURS);
        timeUnitAlias.put("d",  TimeUnit.DAYS);
    }

    public static long toMillis(String rawStr) {
        if (rawStr == null)
            throw new NullPointerException("Null argument is not allowed");

        String str = rawStr.trim() + " ";
        Matcher matcher = MTI_PATTERN.matcher(str);

        if (!matcher.matches())
            throw new IllegalArgumentException(String.format("'%s' doesn't match duration pattern", rawStr));

        matcher = TI_PATTERN.matcher(str);

        BigInteger result = BigInteger.ZERO;

        while (matcher.find()) {
            String unitAlias = matcher.group(TIME_UNIT_GROUP).toLowerCase();
            TimeUnit timeUnit = timeUnitAlias.get(unitAlias);

            if (timeUnit == null) {
                throw new IllegalArgumentException(String.format("Unknown time unit alias '%s' in '%s'", unitAlias, rawStr));
            }

            long summand;
            try {
                summand = Long.valueOf(matcher.group(DURATION_GROUP));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Number overflow for duration '" + rawStr + "'", e);
            }

            result = result.add(BigInteger.valueOf(timeUnit.toMillis(summand)));
        }

        if (result.compareTo(LONG_MAX) == 1)
            throw new IllegalArgumentException("Number overflow for duration '" + rawStr + "'");

        return result.longValue();
    }

    @Override
    public Long convert(String value) {
        return toMillis(value);
    }
}
