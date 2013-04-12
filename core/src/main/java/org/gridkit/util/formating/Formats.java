package org.gridkit.util.formating;

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
	}
}
