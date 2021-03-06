package org.simplemes.mes.demand

import groovy.transform.ToString
import org.simplemes.eframe.data.ChoiceListInterface
import org.simplemes.eframe.data.ChoiceListItemInterface
import org.simplemes.eframe.data.EncodedTypeInterface
import org.simplemes.eframe.data.EncodedTypeListUtils
import org.simplemes.eframe.data.FieldDefinitionInterface
import org.simplemes.eframe.i18n.GlobalUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the basic status codes needed for Orders.  This Status controls the overall status of the order.
 * Sub-elements of the order may have other states or statuses.
 *
 * <h3>Core Statuses</h3>
 * The core statuses for Orders include:
 * <ul>
 *   <li>{@link OrderCreatedStatus}</li>
 *   <li>{@link OrderReadyStatus} - <b>Default</b></li>
 *   <li>{@link OrderHoldStatus}</li>
 *   <li>{@link OrderDoneStatus}</li>
 * </ul>
 *
 */
@ToString(includeNames = true, includePackage = false, excludes = ['value'])
abstract class OrderStatus implements EncodedTypeInterface, ChoiceListInterface, ChoiceListItemInterface {

  /**
   * The max length of the database representation of this status 8.
   */
  public static final int ID_LENGTH = 8

  /**
   * A list of classes that are valid Basic Statuses.
   */
  @SuppressWarnings("unused")
  static List<Class> coreValues = [OrderCreatedStatus, OrderReadyStatus, OrderHoldStatus, OrderDoneStatus]

  /**
   * True if this choice is the default choice in the list.
   */
  Boolean defaultChoice = false

  /**
   * Returns true if this status means that the object is workable.
   * @return True if workable.
   */
  abstract boolean isWorkable()

  /**
   * Returns true if this status means that the object is done.
   * @return True if done.
   */
  abstract boolean isDone()

  /**
   * Returns the instance for the given DB ID.
   * @param id The ID (e.g. 'CREATED').
   * @return The corresponding status (can be null if ID is not valid or null).
   */
  static OrderStatus valueOf(String id) {
    def entry = EncodedTypeListUtils.instance.getAllValues(this).find { it.instance.id == id }
    return entry?.instance
  }

  /**
   * Returns the list of valid values for those formats that use a combobox or similar widget.
   * @param fieldDefinition The field definition used to define this field (optional, provided additional details such as valid values).
   * @return The list of valid values.
   */
  static List<ChoiceListItemInterface> getValidValues(FieldDefinitionInterface fieldDefinition) {
    return EncodedTypeListUtils.instance.getAllValues(OrderStatus)
  }

  /**
   * Returns the localized display value for the choice.
   * @param locale The locale to use for the localized string (<b>Default:</b> default locale).
   * @return The display value.
   */
  @Override
  String toStringLocalized(Locale locale = null) {
    return GlobalUtils.lookup("orderStatus.${id}.label", locale)
  }

  /**
   * Returns the default status.
   * @return The default status.
   */
  static OrderStatus getDefault() {
    // TODO: Delete if not needed by new Choice list approach
    return OrderReadyStatus.instance
  }

  /**
   * Returns the standard done status.
   * @return The done status.
   */
  static OrderStatus getDoneStatus() {
    return OrderDoneStatus.instance
  }

  /**
   * Returns the value for the choice list (e.g. a domain record of enum element).
   * @return The value.
   */
  @Override
  Object getValue() {
    return this
  }

  /**
   * Returns true if this choice is the default choice in the list.
   * @return True if it is the default.
   */
  @Override
  boolean isDefaultChoice() {
    return this.defaultChoice
  }

}

