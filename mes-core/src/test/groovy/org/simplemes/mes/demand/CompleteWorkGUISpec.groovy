package org.simplemes.mes.demand

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.misc.NumberUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.page.WorkCenterSelectionDashboardPage
import org.simplemes.mes.test.MESUnitTestUtils
import org.simplemes.mes.tracking.domain.ActionLog
import org.simplemes.mes.tracking.domain.ProductionLog
import spock.lang.IgnoreIf

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
@IgnoreIf({ !sys['geb.env'] })
class CompleteWorkGUISpec extends BaseDashboardSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order]

  def "verify that complete activity works - order level - complete all to done"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/completeActivity'])

    and: 'an order with all qty in work'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.2)

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the complete is performed'
    clickDashboardButton(0)
    waitForRecordChange(order)

    then: 'the order is updated in the DB'
    def order2 = Order.findByUuid(order.uuid)
    order2.qtyInQueue == 0.0
    order2.qtyInWork == 0.0
    order2.qtyDone == order2.qtyReleased

    and: 'the info message displays the start message'
    //completedToDone.message=Completed quantity {1} for {0}.  All work is done.
    messages.text() == lookup('completedToDone.message', null, order.order, NumberUtils.formatNumber(order2.qtyDone))
  }

  def "verify that complete activity works - order level - complete partial to done"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/completeActivity'])

    and: 'an order with some of the qty in work'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.0)

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the complete is performed'
    clickDashboardButton(0)
    waitForRecordChange(order)

    then: 'the order is updated in the DB'
    def order2 = Order.findByUuid(order.uuid)
    order2.qtyInQueue == 0.2
    order2.qtyInWork == 0.0
    order2.qtyDone == 1.0

    and: 'the info message displays the start message'
    //completed.message=Completed quantity {1} for {0}.  Status is {2}.
    messages.text() == lookup('completed.message', null, order.order, NumberUtils.formatNumber(order2.qtyDone))
  }

  def "verify that complete activity publishes the ORDER_LSN_STATUS_CHANGED event - order level"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardTestController.DISPLAY_EVENT_ACTIVITY],
                   buttons: ['/work/completeActivity'])

    and: 'an order with all qty in work'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.2)

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the complete is performed'
    clickDashboardButton(0)
    waitFor {
      $('#events').text().contains('ORDER_LSN_STATUS_CHANGED')
    }

    then: 'the event is triggered and contains the correct values'
    def s = $('#events').text()
    def json = Holders.objectMapper.readValue(TextUtils.findLine(s, 'ORDER_LSN_STATUS_CHANGED'),Map)
    json.type == 'ORDER_LSN_STATUS_CHANGED'
    json.source == '/work/completeActivity'
    List list = json.list
    list.size() == 1
    list[0].order == order.order
  }

  def "verify that complete activity creates the correct undo action and the undo works"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/completeActivity'])
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.2)

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the complete is performed'
    clickDashboardButton(0)
    waitForRecordChange(order)

    and: 'the undo is triggered'
    assert undoButtonEnabled
    undoButton.click()
    waitForRecordChange(order)

    then: 'the order is updated in the DB'
    def order2 = Order.findByUuid(order.uuid)
    order2.qtyInQueue == order2.qtyReleased
    order2.qtyInWork == 0.0

    and: 'the info message displays the start message'
    //reversedComplete.message=Reversed Complete of quantity {1} for {0}.
    messages.text() == lookup('reversedComplete.message', null, order.order, NumberUtils.formatNumber(order2.qtyInQueue))
  }

  def "verify that complete activity gracefully handles missing order"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/completeActivity'])

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the complete is performed'
    clickDashboardButton(0)
    waitFor {
      messages.text()
    }

    then: 'the message is correct'
    messages.text() == lookup('orderLSN.missing.message')

    and: 'is an error'
    messages.isError()
  }

  def "verify that complete activity gracefully handles server-side error"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: ['/work/completeActivity'])

    and: 'an order with a hold status that can not be worked'
    def order = MESUnitTestUtils.releaseOrder(qty: 1.2, qtyInWork: 1.2)
    Order.withTransaction {
      order.overallStatus = OrderHoldStatus.instance
      order.save()
    }

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'the order is filled in'
    orderLSNField.input.value(order.order)

    and: 'the complete is performed'
    clickDashboardButton(0)
    waitFor {
      messages.text()
    }

    then: 'the message is correct'
    //error.3001.message=Order {0} cannot be processed. It has a status of {1}.
    messages.text().contains(lookup('error.3001.message', null, order.order, order.overallStatus.toStringLocalized()))
    messages.isError()
  }

}