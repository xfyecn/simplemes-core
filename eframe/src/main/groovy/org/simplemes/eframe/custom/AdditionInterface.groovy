/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom
/**
 * Defines the methods an addition class must provide.  These additions define how modules add features to an
 * application.
 */
interface AdditionInterface {

  /**
   * Returns the name of this addition.
   * @return The name.
   */
  String getName()

  /**
   * Returns the list of encoded field types that will be supported.
   * @return The list of classes.
   */
  List<Class> getEncodedTypes()

  /**
   * Returns a list of classes that define non-domain classes that will perform initial data loading.
   * These classes need a static initialDataLoad() method.
   * @return The list of other classes
   */
  List<Class> getInitialDataLoaders()

  /**
   * Returns the elements needed/provided by this addition.
   *
   * @return The addition configuration.
   */
  AdditionConfiguration getAddition()

  /**
   * Returns the field extensions defined in this addition.
   *
   * @return The field additions.
   */
  List<AdditionFieldConfiguration> getFields()

  /**
   * Returns the assets defined in this addition.
   *
   * @return The asset additions.
   */
  List<AdditionAssetConfiguration> getAssets()

}