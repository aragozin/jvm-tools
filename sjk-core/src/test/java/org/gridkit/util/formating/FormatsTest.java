package org.gridkit.util.formating;

import org.junit.Test;

import static org.gridkit.util.formating.Formats.*;
import static org.junit.Assert.*;

public class FormatsTest {

  @Test
  public void testformatRate() {
    assertEquals("    0", formatRate(0d));
    assertEquals("50.00", formatRate(50d));
    assertEquals("500.0", formatRate(500d));
    assertEquals("50.0k", formatRate(50e3d));
    assertEquals("50.0m", formatRate(50e6d));
    assertEquals("50.0g", formatRate(50e9d));
    assertEquals("1.5e+11", formatRate(150e9d));
  }

  @Test
  public void testToMemorySize() {
    assertEquals("3", toMemorySize(3L));
    assertEquals("38k", toMemorySize(39327L));
    assertEquals("31m", toMemorySize(33553116L));
    assertEquals("35g", toMemorySize(38506017581L));
  }
}
