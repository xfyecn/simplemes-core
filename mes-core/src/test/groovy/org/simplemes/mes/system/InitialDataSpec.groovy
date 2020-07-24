package org.simplemes.mes.system

import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test.
 *
 */
class InitialDataSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  @Rollback
  def "tests createRoles()"() {
    when: 'initial data is loaded'
    def res = InitialData.initialDataLoad()

    then: 'the roles exist'
    Role.findByAuthority('SUPERVISOR')
    Role.findByAuthority('ENGINEER')
    Role.findByAuthority('LEAD')
    Role.findByAuthority('OPERATOR')

    and: 'the records listed are correct'
    res.Role.containsAll(['SUPERVISOR', 'ENGINEER', 'LEAD', 'OPERATOR'])
  }

  @Rollback
  def "tests createDashboards()"() {
    given: 'the dashboards will be loaded even in test mode'
    InitialData.forceDashboardLoad = true

    when: 'initial data is loaded'
    InitialData.createDashboards()

    then: 'the dashboards exist'
    DashboardConfig.findByDashboard('OPERATOR_DEFAULT')
    DashboardConfig.findByDashboard('MANAGER_DEFAULT')

    cleanup: 'reset from test mode'
    InitialData.forceDashboardLoad = false

  }
}