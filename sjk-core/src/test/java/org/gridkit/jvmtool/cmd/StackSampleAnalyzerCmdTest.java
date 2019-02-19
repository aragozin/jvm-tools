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
  public void testDateFormaterToString() {
    Assert.assertEquals("", new DateFormater(null).toString(null));
    Assert.assertEquals("2019.02.18_17:17:14", new DateFormater(TimeZone.getDefault()).toString(1550510234000L));
    thrown.expect(NullPointerException.class);
    new DateFormater(null).toString(0L);
    // Exception thrown
  }

  @Test
  public void testDecimalFormaterToString() {
    Assert.assertEquals("", new DecimalFormater(9).toString(null));
    Assert.assertEquals("", new DecimalFormater(9).toString("text"));
    Assert.assertEquals("\t4", new DecimalFormater(9).toString(4L));
    Assert.assertEquals("\t4.000000000", new DecimalFormater(9).toString(4));
  }

  @Test
  public void testMemRateFormaterToString() {
    Assert.assertEquals("", new MemRateFormater().toString(null));
    Assert.assertEquals("", new MemRateFormater().toString(Double.NaN));
    Assert.assertEquals("0/s", new MemRateFormater().toString(0));
    Assert.assertEquals("9/s", new MemRateFormater().toString(9));
    Assert.assertEquals("19k/s", new MemRateFormater().toString(20475));
    Assert.assertEquals("70m/s", new MemRateFormater().toString(74160839));
    Assert.assertEquals("10g/s", new MemRateFormater().toString(10737418240l));
  }

}
