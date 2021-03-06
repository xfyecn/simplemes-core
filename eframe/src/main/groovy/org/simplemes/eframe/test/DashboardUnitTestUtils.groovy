/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import org.simplemes.eframe.dashboard.domain.DashboardButton
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.dashboard.domain.DashboardPanel
import org.simplemes.eframe.dashboard.domain.DashboardPanelSplitter

/**
 * Utility methods to help test Dashboard features.
 *
 *
 */
class DashboardUnitTestUtils {
  /**
   * Defines a dashboard config for testing.
   * <b>Note:</b> You should cleanup these records with the static variable:
   * <pre>
   * static domainClassesToClearOnEachRun = [DashboardConfig]
   * </pre>
   * @param dashboardName The dashboard name.
   * @param defaultPages The default pages for each panel (if starts with 'vertical' or 'horizontal', then creates a splitter with later panels).
   * @param buttons The buttons.  An Array of Maps with elements that match the DashboardButton fields: [[label:'',url:'page1', panel: 'A']]
   * @return The configuration.
   */
  static DashboardConfig buildDashboardConfig(String dashboardName, List<String> defaultPages, List<Map> buttons = null) {
    DashboardConfig dashboardConfig = new DashboardConfig(dashboard: dashboardName, title: "title-$dashboardName")
    int index = 0
    int lastSplitterIndex = -1
    for (page in defaultPages) {
      if (page.startsWith('vertical') || page.startsWith('horizontal')) {
        boolean vertical = page.startsWith('vertical')
        def splitter = new DashboardPanelSplitter(panelIndex: index, vertical: vertical, parentPanelIndex: lastSplitterIndex)
        lastSplitterIndex = index
        dashboardConfig.splitterPanels << splitter
        index++
      } else {
        if (page == '') {
          page = null
        }
        def panel = new DashboardPanel(panelIndex: index, defaultURL: page, parentPanelIndex: lastSplitterIndex)
        dashboardConfig.dashboardPanels << panel
        index++
      }
    }
    // Now, create the buttons (if any)
    for (button in buttons) {
      if (button instanceof Map) {
        // Must handle child Map separately since in functional (GEB) test mode, the child activities are not saved.
        def dashboardButton = new DashboardButton(button)
        dashboardConfig.buttons << dashboardButton
      } else if (button instanceof List) {
        // Multiple activities on one button.  Save them as a new record in button list with the same ID.
        int sequence = 1
        List<Map> list = button
        for (activity in list) {
          // Must handle child Map separately since in functional (GEB) test mode, the child activities are not saved.
          activity.sequence = activity.sequence ?: sequence
          def dashboardButton = new DashboardButton(activity)
          dashboardConfig.buttons << dashboardButton
          sequence++
        }
      }
    }

    dashboardConfig.save()
    //println "dashboardConfig = ${dashboardConfig.toString()}"
    //println "dashboardConfig = ${dashboardConfig.hierarchyToString()}"
    return dashboardConfig
  }

}
