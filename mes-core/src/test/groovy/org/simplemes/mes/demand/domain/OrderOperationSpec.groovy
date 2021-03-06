package org.simplemes.mes.demand.domain

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class OrderOperationSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

def "test constraints"() {
  given: 'an oper'
  def order = new Order()

  expect: 'the constraints are enforced'
  DomainTester.test {
    domain OrderOperation
    requiredValues title: 'n', order: order, sequence: 1
    maxSize 'title', FieldSizes.MAX_TITLE_LENGTH
    fieldOrderCheck false
  }
}

  @Rollback
  def "copy constructor works"() {
    given: 'a routing operation'
    def op1 = new OrderOperation(sequence: 100, title: 'XYZ')

    when: 'the copy constructor is used'
    def op2 = new OrderOperation(op1)

    then: 'the fields are copied'
    op1.sequence == op2.sequence
    op1.title == op2.title
    op1 <=> op2 == 0
  }

  @Rollback
  def "copy constructor works with custom fields"() {
    given: 'custom field on the operation'
    DataGenerator.buildCustomField(fieldName: 'color', domainClass: OrderOperation)

    and: 'a routing operation with a custom field value'
    def op1 = new OrderOperation(sequence: 100, title: 'XYZ')
    op1.setFieldValue('color', 'green')

    when: 'the copy constructor is used'
    def op2 = new OrderOperation(op1)

    then: 'the custom field is copied'
    op2.getFieldValue('color') == 'green'
  }
}
