/**
 * Copyright 2012 Alexey Ragozin
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

import java.text.SimpleDateFormat;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class Formats {

	public static final String ZERO_LEAD_DECIMAL_6 = "%06d";
	public static final String DATE_STAMP = "%1$tFT%1$tT.%1$tL%1$tz";
	public static final String FILE_DATE_STAMP = "%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS";
	public static final String TIME_STAMP = "%1$tT.%1$tL";
	
	public static final String toTimestamp(long millis) {
		return String.format(TIME_STAMP, millis);	
	}

	public static final String toDatestamp(long millis) {
		return String.format(DATE_STAMP, millis);	
	}

	public static final String toFileDatestamp(long millis) {
		return String.format(FILE_DATE_STAMP, millis);	
	}	

	public static final String currentDatestamp() {
		return toDatestamp(System.currentTimeMillis());
	}

	public static final String currentFileDatestamp() {
		return toFileDatestamp(System.currentTimeMillis());
	}
	
	public static final String formatRate(double rate) {
		if (rate == 0) {
			return "    0";
		}
		else if (rate < 100) {
			return String.format("%5.2f", rate);
		}
		else if (rate < 1000) {
			return String.format("%5.1f", rate);
		}
		else if (rate < 100e3) {
			return String.format("%4.1fk", rate / 1e3);
		}
		else if (rate < 100e6) {
			return String.format("%4.1fm", rate / 1e6);
		}
		else if (rate < 100e9) {
			return String.format("%4.1fg", rate / 1e9);
		}
		else {
			return String.format("%5.1e", rate);
		}
	}
	
	public static final String toMemorySize(long n) {
		if (n < (10l << 10)) {
			return String.valueOf(n);
		}
		else if (n < (10l << 20)) {
			return String.valueOf(n >> 10) + "k";
		}
		else if (n < (10l << 30)) {
			return String.valueOf(n >> 20) + "m";
		}
		else {
			return String.valueOf(n >> 30) + "g";
		}
	}
	
	public static void main(String[] args) {
		System.out.println("DATE_STAMP: " + String.format(DATE_STAMP, System.currentTimeMillis()));
		System.out.println("FILE_DATE_STAMP: " + String.format(FILE_DATE_STAMP, System.currentTimeMillis()));
		System.out.println("ZERO_LEAD_DECIMAL_6: " + String.format(ZERO_LEAD_DECIMAL_6, 1234));
		System.out.println(String.format("[%-6s]", "x"));
		System.out.println(String.format("[%.2f]", 0.1));
		System.out.println(String.format("[%5.2f]", 0.1));
		System.out.println(String.format("[%05.2f]", 0.1));
		System.out.println(String.format("[%5.2f]", 0f));
		System.out.println(String.format("[%5.2f]", 10.11));
		System.out.println(String.format("[%5.2f]", 100.11));
		System.out.println(String.format("[%5.2f]", 1000.11));
		System.out.println(String.format("[%5.02g]", 1000.11));
		System.out.println("SimpleDataFormat: " + new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss").format(System.currentTimeMillis()));
	}
}
