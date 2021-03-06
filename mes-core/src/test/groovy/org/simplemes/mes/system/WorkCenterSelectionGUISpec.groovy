package org.simplemes.mes.system

import org.openqa.selenium.Keys
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.controller.DashboardTestController
import org.simplemes.eframe.test.BaseDashboardSpecification
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.page.WorkCenterSelectionDashboardPage
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.Product
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
class WorkCenterSelectionGUISpec extends BaseDashboardSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, ProductionLog, Order, Product]

  def "verify that work center passed on URL is used by activity and the page is localized"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'])

    when: 'the dashboard is displayed'
    displayDashboard([workCenter: 'WC137'])

    then: 'the passed in work center is displayed'
    $('#workCenter').text() == 'WC137'

    and: 'the work center field is labeled correctly'
    $('#workCenterLabel').text() == lookup('workCenter.label')

    and: 'the input field is display'
    textField("order").label == lookup('orderLsn.label')
  }

  def "verify that work center change dialog allows change and value is re-used on refresh"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the dialog is opened'
    $('#workCenter').click()
    waitFor {
      dialog0.exists
    }

    then: 'the work center field is labeled correctly'
    def field = textField("wcdChangeWorkCenter")
    field.label == lookupRequired('workCenter.label')

    when: 'the value is change and saved'
    field.input.value('XYZ')
    button('dialog0-ok').click()
    waitFor {
      !(dialog0.exists)
    }

    and: 'the page is re-displayed'
    displayDashboard()

    then: 'the previous work center is used'
    $('#workCenter').text() == 'XYZ'
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that the selection activity sends the ORDER_LSN_CHANGED event"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    when: 'the dashboard is displayed'
    displayDashboard([workCenter: 'WC137'])

    then: 'the order is changed'
    textField('order').input.value('ORDER1')
    sendKey(Keys.TAB)
    waitFor {
      $('#events').text().contains('ORDER_LSN_CHANGED')
    }

    and: 'the event is triggered'
    def s = $('#events').text()
    def json = Holders.objectMapper.readValue(s, Map)
    json.type == 'ORDER_LSN_CHANGED'
    json.source == '/selection/workCenterSelection'
    json.list.size() == 1
    json.list[0].order == 'ORDER1'
  }

  def "verify that the selection activity sends the WORK_CENTER_CHANGED event"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardTestController.DISPLAY_EVENT_ACTIVITY])

    when: 'the dashboard is displayed'
    displayDashboard([workCenter: 'WC137'])

    and: 'the dialog is opened'
    $('#workCenter').click()
    waitFor {
      dialog0.exists
    }

    and: 'the value is change and saved'
    def field = textField("wcdChangeWorkCenter")
    field.input.value('XYZ')
    button('dialog0-ok').click()
    waitFor {
      $('#events').text().contains('WORK_CENTER_CHANGED')
    }

    then: 'the event is triggered and contains the correct values'
    def s = $('#events').text()
    def json = Holders.objectMapper.readValue(s,Map)
    json.type == 'WORK_CENTER_CHANGED'
    json.source == '/selection/workCenterSelection'
    json.workCenter == 'XYZ'
  }

  def "verify that the selection activity provides the order and work center as a parameter for other activities"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardTestController.EMPTY_ACTIVITY],
                   buttons: [DashboardTestController.DISPLAY_PARAMETERS_ACTIVITY])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the dialog is opened'
    $('#workCenter').click()
    waitFor {
      dialog0.exists
    }

    and: 'the value is change and saved'
    def field = textField("wcdChangeWorkCenter")
    field.input.value('XYZ')
    button('dialog0-ok').click()
    waitFor {
      !dialog0.exists
    }

    and: 'the order is set'
    textField("order").input.value('ORDER237')


    and: 'the display parameters activity is displayed'
    clickDashboardButton(0)
    waitForCompletion()

    then: 'the parameter is displayed with the correct values - current work center value'
    def s = $('#parameters').text()
    s.contains('workCenter=XYZ')
    s.contains('order=ORDER237')
  }

  def "verify that the selection activity handles the WORK_LIST_SELECTED event - single Order case"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardTestController.TRIGGER_EVENT_ACTIVITY])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the event is triggered'
    def event = [type: 'WORK_LIST_SELECTED', source: 'abc', list: [[order: 'ORDER1']]]
    textField("eventSource").input.value(Holders.objectMapper.writeValueAsString(event))
    clickButton('triggerEvent')
    waitFor {
      textField('order').input.value() == 'ORDER1'
    }

    then: 'the field is correct'
    textField('order').input.value() == 'ORDER1'
  }

  def "verify that the selection activity handles the WORK_LIST_SELECTED event - multiple Order case"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection', DashboardTestController.TRIGGER_EVENT_ACTIVITY])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'the event is triggered'
    def event = [type: 'WORK_LIST_SELECTED', source: 'abc', list: [[order: 'ORDER1'], [order: 'ORDER2']]]
    textField("eventSource").input.value(Holders.objectMapper.writeValueAsString(event))
    clickButton('triggerEvent')
    waitFor {
      textField('order').input.value() == lookup('multiplesSelected.label')
    }

    then: 'the field is correct'
    textField('order').input.value() == lookup('multiplesSelected.label')
  }

  def "verify that activity state store - restore works"() {
    given: 'a dashboard with the activity and a second GUI activity'
    def guiActivity2 = '''
    <script>
      <@efForm id="logFailure" dashboard="buttonHolder">  
        <@efButtonGroup>
          <@efButton id='doneButton' label="Done" click="dashboard.finished('${params._panel}')"/>  
        </@efButtonGroup>  
      </@efForm>
    </script>
    '''
    buildDashboard(defaults: ['/selection/workCenterSelection'], buttons: [guiActivity2])

    when: 'the dashboard is displayed'
    displayDashboard()

    and: 'a user input is made'
    $('#order').value('XYZZY')

    and: 'the second activity is displayed'
    clickDashboardButton(0)
    waitForCompletion()

    and: 'the second activity is dismissed'
    clickButton('doneButton')
    waitForCompletion()

    //sleep(20000)
    then: 'a user input is restored'
    $('#order').value() == 'XYZZY'
  }

  def "verify that the suggest on order works"() {
    given: 'a dashboard with the activity'
    buildDashboard(defaults: ['/selection/workCenterSelection'])

    and: 'some orders to display in the suggest list'
    WorkCenter.withTransaction {
      setCurrentUser()
      MESUnitTestUtils.releaseOrders(nOrders: 5, spreadQueuedDates: true)
    }

    when: 'the dashboard is displayed'
    displayDashboard(page: WorkCenterSelectionDashboardPage)

    and: 'a value is started'
    orderLSNField.input.click()
    sendKey('M')
    waitFor {
      $('div.webix_popup', view_id: '$suggest1').displayed
    }

    then: 'suggest works'
    $('div.webix_popup', view_id: '$suggest1').text().contains('M1002')
  }

}