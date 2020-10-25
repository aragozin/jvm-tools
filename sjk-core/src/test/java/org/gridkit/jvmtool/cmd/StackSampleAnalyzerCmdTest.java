package org.gridkit.jvmtool.cmd;

import org.gridkit.jvmtool.cmd.StackSampleAnalyzerCmd.DateFormater;
import org.gridkit.jvmtool.cmd.StackSampleAnalyzerCmd.DecimalFormater;
import org.gridkit.jvmtool.cmd.StackSampleAnalyzerCmd.MemRateFormater;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.TimeZone;

public class StackSampleAnalyzerCmdTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testNullTimeZoneDateFormaterToString(){
    Assert.assertEquals("", new DateFormater(null).toString(null));
  }

  @Test
  public void testDateFormaterToString(){
    Assert.assertEquals("2019.02.18_17:17:14", new DateFormater(TimeZone.getTimeZone("UTC")).toString(1550510234000L));
  }

  @Test
  public void testDateFormaterToStringException() {
    thrown.expect(NullPointerException.class);
    new DateFormater(null).toString(0L);
    // Exception thrown
  }

  @Test
  public void testDecimalFormaterToStringNullParam() {
    Assert.assertEquals("", new DecimalFormater(9).toString(null));
  }

  @Test
  public void testDecimalFormaterToStringNonNumericParam() {
    Assert.assertEquals("", new DecimalFormater(9).toString("text"));
  }

  @Test
  public void testDecimalFormaterToStringLongParam() {
    Assert.assertEquals("\t4", new DecimalFormater(9).toString(4L));
  }

  @Test
  public void testDecimalFormaterToStringNumericParam() {
    Assert.assertEquals("\t4", new DecimalFormater(9).toString(4));
  }

  @Test
  public void testDecimalFormaterToStringNumericParam2() {
    Assert.assertEquals("\t4.0000001", new DecimalFormater(9).toString(4.0000001));
  }

  @Test
  public void testDecimalFormaterToStringNumericParam3() {
    Assert.assertEquals("\t4", new DecimalFormater(2).toString(4.0000001));
  }

  @Test
  public void testMemRateFormaterToStringNullParam() {
    Assert.assertEquals("", new MemRateFormater().toString(null));
  }

  @Test
  public void testMemRateFormaterToStringNaNParam() {
    Assert.assertEquals("", new MemRateFormater().toString(Double.NaN));
  }

  @Test
  public void testMemRateFormaterToStringNoRate() {
    Assert.assertEquals("0/s", new MemRateFormater().toString(0));
  }

  @Test
  public void testMemRateFormaterToString() {
    Assert.assertEquals("9/s", new MemRateFormater().toString(9));
  }

  @Test
  public void testMemRateFormaterToStringKps() {
    Assert.assertEquals("19k/s", new MemRateFormater().toString(20475));
  }

  @Test
  public void testMemRateFormaterToStringMps() {
    Assert.assertEquals("70m/s", new MemRateFormater().toString(74160839));
  }

  @Test
  public void testMemRateFormaterToStringGps() {
    Assert.assertEquals("10g/s", new MemRateFormater().toString(10737418240L));
  }

}
