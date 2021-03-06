/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.simplemes.eframe.reports;

import org.junit.jupiter.api.Test;
import org.simplemes.eframe.reports.ReportUtils;

import static org.junit.jupiter.api.Assertions.*;

class ReportUtilsTest {
  @Test
  void testFormatFields() {
    String s = "{\"assemblyData_LOT\": \"ACME14036\" }";
    assertEquals("{ LOT :  ACME14036  }", ReportUtils.formatFields(s,"","assemblyData",true, null));
  }
}
