package org.simplemes.mes.demand


import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.demand.domain.LSNSequence
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.demand.service.OrderService
import org.simplemes.mes.product.domain.Product
import org.simplemes.mes.tracking.domain.ActionLog

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests Release/Start/Complete cycle for performance.
 */
class WorkCycleE2ESpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [ActionLog, Order, Product, LSNSequence]

  def setup() {
    setCurrentUser()
  }

  @SuppressWarnings("JavaIoPackageAccess")
  def "test release with no LSNs"() {
    given: 'a test'
    def logData = new File('tmp').isDirectory()
    def file = new File('tmp/perfTests.txt')
    if (logData) {
      if (!file.exists()) {
        file << "date,release,start,complete\n"
      }
    }

    when: 'the order is released and worked'
    def nTestsTotal = 100
    long total1 = 0
    long total2 = 0
    long total3 = 0
    for (i in (1..nTestsTotal)) {
      def order1 = null
      Order.withTransaction {
        order1 = new Order(order: "M100$i", qtyToBuild: 10).save()
      }
      def start1 = System.currentTimeMillis()
      Order.withTransaction {
        new OrderService().release(new OrderReleaseRequest(order1))
      }

      def elapsed1 = System.currentTimeMillis() - start1
      def start2 = System.currentTimeMillis()
      Order.withTransaction {
        order1.startQty(10.0)
      }
      def elapsed2 = System.currentTimeMillis() - start2

      def start3 = System.currentTimeMillis()
      Order.withTransaction {
        order1.completeQty(10.0)
      }
      def elapsed3 = System.currentTimeMillis() - start3
      total1 += elapsed1
      total2 += elapsed2
      total3 += elapsed3

      //println "times  $elapsed1, $elapsed2, $elapsed3"
      if (logData) {
        file << "${ISODate.format(new DateOnly())},$elapsed1, $elapsed2, $elapsed3\n"
      }

    }
    def rel = total1 / nTestsTotal
    def start = total2 / nTestsTotal
    def comp = total3 / nTestsTotal
    //System.out.println("average release/start/complete = ${rel},${start},${comp} ms")
    if (logData) {
      def file2 = new File('tmp/perfTestAvg.csv')
      if (!file2.exists()) {
        file2 << "date,release,start,complete\n"
      }
      file2 << "${ISODate.format(new DateOnly())},$rel, $start, $comp\n"
    }

    then: ''
  }

}
