package org.simplemes.mes.demand

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The order is done.
 */
class OrderDoneStatus extends OrderStatus {
  /**
   * The singleton-like instance.
   */
  static OrderDoneStatus instance = new OrderDoneStatus()

  /**
   * The database representation of this status.
   */
  public static final String ID = 'DONE'

  /**
   * Returns true if this status means that the object is workable.
   * @return True if workable.
   */
  @Override
  boolean isWorkable() {
    return false
  }

  /**
   * Returns true if this status means that the object is done.
   * @return True if done.
   */
  @Override
  boolean isDone() {
    return true
  }

  /**
   * Returns the encoded ID of this value.
   * @return The encoded value.
   */
  @Override
  String getId() {
    return ID
  }
}
