/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

/**
 * Defines the GEB Page for the framework's standard dashboard page.
 * <p/>
 */
@SuppressWarnings("GroovyUnusedDeclaration")
class DashboardPage extends AbstractPage {
  /**
   * Build the page.
   */
  DashboardPage() {
    // No need to wait for Ajax finish on load.
  }

  static url = "/dashboard"
  static at = { title.contains(lookup('dashboard.label')) }
  //static at = { true }

  /**
   * The page content available for this page.  See above.
   */
  static content = {
    panel { id -> $('div.webix_form', view_id: "Content$id") }
    undoButton { $('#undoButton') }
    undoButtonEnabled { !$('#undoButton').classes().contains('undo-button-disabled') }
  }

  /**
   * If true, then the page will wait on load until the Ajax queries are completed.
   * Override in your sub-class if you have Ajax loading mechanism.
   * This parent class sets it to false.
   * <p>
   * <b>Note:</b> This wait for Ajax completion requires the eframe_toolkit.js be loaded.
   */
  boolean getWaitForAjaxOnLoad() {
    return true
  }

}
