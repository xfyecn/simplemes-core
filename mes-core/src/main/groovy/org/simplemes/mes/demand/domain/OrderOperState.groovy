package org.simplemes.mes.demand.domain

import groovy.transform.EqualsAndHashCode
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.mes.demand.WorkStateTrait
import org.simplemes.mes.demand.WorkableInterface
import org.simplemes.mes.product.OperationTrait
import org.simplemes.mes.product.RoutingUtils

import javax.annotation.Nullable
import javax.persistence.ManyToOne

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This object holds the work state of an operation for an order.  This domain uses the WorkStateTrait to provide
 * most of the logic and fields needed.
 * <p/>
 * This WorkStateTrait contains the quantities in queue, work and done.
 * This object also implements the WorkableInterface to encapsulate the process of working
 * on various objects.
 */
@MappedEntity
@DomainEntity
@SuppressWarnings('unused')
@EqualsAndHashCode(includes = ['order', 'sequence'])
class OrderOperState implements WorkStateTrait, WorkableInterface {
  /**
   * A copy of the sequence for the operation this state applies to.
   */
  int sequence

  /**
   * This operation state always belongs to a single Order.
   */
  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  Order order

  /**
   * The number of pieces waiting to be worked (in queue) for this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyInQueue = 0.0

  /**
   * The number of pieces actively in work for this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyInWork = 0.0

  /**
   * The number of pieces to completed (done) on this object.
   * <p/><b>WorkStateTrait field</b>.
   */
  BigDecimal qtyDone = 0.0

  /**
   * The date/time any quantity was queued at this point (operation or top-level).
   * Can be null if the nothing is in queue.
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateQtyQueued

  /**
   * The date/time any quantity was started at this point (operation or top-level).
   * Can be null if the nothing is in work.
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateQtyStarted

  /**
   * The date/time any quantity was first queued at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateFirstQueued

  /**
   * The date/time any quantity was first started at this point (operation or top-level).
   * <p/><b>WorkStateTrait field</b>.
   */
  @Nullable @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateFirstStarted

  /**
   * The custom field holder.
   */
  @Nullable
  @ExtensibleFieldHolder
  @MappedProperty(type = DataType.JSON)
  String fields

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   * The empty constructor.
   */
  OrderOperState() {
    setDatesAsNeeded()
  }

  /**
   * A copy constructor to copy the operation info from another operation.
   * @param operation The routing to copy from.
   */
  OrderOperState(OperationTrait operation) {
    ArgumentUtils.checkMissing(operation, "operation")
    this.sequence = operation.sequence
    setDatesAsNeeded()
  }

  /**
   * Determines the next workable to be performed after this workable is completed.
   * This operation state delegates to the order/routing to figure this out.
   * @return The workable.  Null if there is no other workable left to be processed.
   */
  WorkableInterface determineNextWorkable() {
    return order?.determineNextWorkable(this)
  }

  /**
   * Saves any changes to this record.
   */
  void saveChanges() {
    save()
  }

  /**
   * Provides a human readable string for this object.
   * @return The string.
   */
  @Override
  String toString() {
    return RoutingUtils.combineKeyAndSequence(order?.order, sequence) + " " + qtyInQueue + "/" + qtyInWork
  }
}
