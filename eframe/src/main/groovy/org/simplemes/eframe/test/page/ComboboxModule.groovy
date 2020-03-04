/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.page

import geb.Module

/**
 * Defines the GEB page elements for a standard editable combobox field.  This contains these elements:
 * <p>
 * <h4>Example Page Definition:</h4>
 * <pre>
 *   static content = &#123;
 *     fieldFormat &#123; module(new ComboboxModule(field: 'fieldFormat')) &#125;
 *   &#125;
 * </pre>
 *
 * <p>
 * <h4>Example Test Spec Usage:</h4>
 * <pre>
 *   fieldFormat.input.value('STRING')
 *   fieldFormat.input.value = 'INTEGER'
 * </pre>
 *
 * <h4>This contains these elements:</h4>
 * <ul>
 *   <li><b>label</b> - The label text.</li>
 *   <li><b>input</b> - The input field itself.</li>
 *   <li><b>invalid</b> - True if the input field is marked as invalid (css makes it appear red).</li>
 *   <li><b>popupOpen</b> - True if the combobox list is displayed (open).</li>
 * </ul>
 *
 */
@SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedDeclaration"])
class ComboboxModule extends Module {
  String field

  static content = {
    label { $('div.webix_el_label', view_id: "${field}Label").text() }
    input { $('div.webix_el_combo', view_id: "${field}").find('input') }
    invalid { $('div.webix_el_combo', view_id: "${field}").classes().contains('webix_invalid') }
    popupOpen { $('div.webix_el_combo', view_id: "${field}").find('input').attr('aria-expanded') == 'true' }
  }

}
