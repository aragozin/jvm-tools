package org.gridkit.util.formating;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;

import org.junit.Test;

public class SimpleNumberFormatterTest {

    @Test
    public void verify_decimal() {
        
        SimpleNumberFormatter snf = new SimpleNumberFormatter("D00.##");
        
        assertThat(snf.formatLong(1000)).isEqualTo("1000");
        assertThat(snf.formatLong(1)).isEqualTo("01");

        assertThat(snf.formatDouble(1000)).isEqualTo("1000");
        assertThat(snf.formatDouble(1)).isEqualTo("01");
        assertThat(snf.formatDouble(1.1)).isEqualTo("01.1");
        assertThat(snf.formatDouble(1.55)).isEqualTo("01.55");
        assertThat(snf.formatDouble(1.5555)).isEqualTo("01.56");
        assertThat(snf.formatDouble(10.5555)).isEqualTo("10.56");
        
    }

    @Test
    public void verify_date_format() {
        
        SimpleNumberFormatter snf = new SimpleNumberFormatter("Tyyyy", TimeZone.getTimeZone("UTC"));
        
        assertThat(snf.formatLong(0)).isEqualTo("1970");
        assertThat(snf.formatDouble(0d)).isEqualTo("1970");
    }

    @Test
    public void verify_string_format() {
        
        SimpleNumberFormatter snf;
        
        snf = new SimpleNumberFormatter("F%d");
        assertThat(snf.formatLong(100)).isEqualTo("100");

        snf = new SimpleNumberFormatter("F%g");
        assertThat(snf.formatDouble(100)).isEqualTo("100.000");
    }
}
