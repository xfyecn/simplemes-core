/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.reports

import groovy.util.logging.Slf4j
import org.simplemes.eframe.data.FieldDefinitions
import org.simplemes.eframe.misc.ClassPathScanner

//import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * Provides helper methods for finding and using reports from the external report engine.
 */
@Slf4j
class ReportHelper {

  /**
   * A singleton-style instance for this class.  Used to make testing easier.
   */
  static ReportHelper instance = new ReportHelper()

  /**
   * The page that serves up the reports.  Used mainly for user preference storage.
   */
  static final String REPORT_PAGE = '/report/index'

  /**
   * Finds the list of available built-in reports from the file system and/or the class path.
   * Ignores .jrxml files that start with underscore (_).
   * @return The list of location strings.
   */
  List<String> determineBuiltinReports() {
    def list = ClassPathScanner.factory.buildScanner('reports/*.jrxml').scan()*.toString()

    // Only use the entries that the basic file name starts with _.
    def res = []
    for (String s in list) {
      def loc = s.lastIndexOf('/')
      if (loc >= 0) {
        def fileName = s[(loc + 1)..-1]
        if (!fileName.startsWith('_')) {
          res << s
        }
      }
    }

    return res
  }

  /**
   * Opens an input stream for the report's jrxml file.  Supports built-in reports found on the classpath.
   * @param reportDetails The report details.
   * @return The input stream.  Can be null if not found.
   */
  @SuppressWarnings("JavaIoPackageAccess")
  InputStream getInputStream(Report reportDetails) {
    def resourcePath = "$reportDetails.reportFolder/$reportDetails.reportName"
    if (resourcePath.contains("jar:")) {
      def url = new URL(resourcePath)
      return url.openStream()
    } else {
      return this.class.getClassLoader().getResourceAsStream(resourcePath)
    }
  }

  /**
   * Determines the base file name for the given report path.  For example,
   * 'reports/Metrics.jrxml' returns 'Metrics'.
   * @param fullPath The full path of the report.
   * @return The base file name.
   */
  String determineReportBaseName(String fullPath) {
    def res = ''

    def loc = fullPath?.lastIndexOf('/')
    if (loc >= 0) {
      res = fullPath[(loc + 1)..-1] - '.jrxml'
    } else if (fullPath) {
      res = fullPath - '.jrxml'
    }

    return res
  }

  /**
   * Builds the FieldDefinitions from the list of report parameters.  This is used for display purposes.
   * @param report The report.
   * @return The FieldDefinitions.
   */
  FieldDefinitions buildFieldDefinitionsFromParameters(Report report) {
    def fieldDefinitions = new FieldDefinitions()
    //def compiledReport = ReportEngine.instance.compile(report)

    for (param in report.reportParameters) {
      fieldDefinitions << param
    }

    return fieldDefinitions
  }


}

