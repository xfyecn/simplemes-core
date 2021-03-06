package org.simplemes.mes.assy.demand

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.assy.demand.domain.OrderAssembledComponent
import org.simplemes.mes.demand.domain.Order

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class RemoveOrderAssembledComponentRequestSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that the copy constructor works"() {
    given: 'an OrderAssembledComponent record'
    def order = new Order(order: 'M1001')
    def comp = new OrderAssembledComponent(sequence: 247)

    when: 'the constructor is called'
    def request = new RemoveOrderAssembledComponentRequest(comp, order)

    then: 'the values are correct'
    request.order == order
    request.sequence == 247
  }

  @Rollback
  def "verify that the Map constructor works - Order"() {
    given: 'an order'
    def order = new Order(order: 'ABC').save()

    when: 'the constructor is used'
    def request = new RemoveOrderAssembledComponentRequest([order: order.order, sequence: 237])

    then: 'the order is used'
    request.order == order
    request.sequence == 237
  }

  @Rollback
  def "verify that the Map constructor gracefully handles missing order"() {
    when: 'the constructor is used'
    def request = new RemoveOrderAssembledComponentRequest([:])

    then: 'the request is created as empty'
    !request.order
  }


}
