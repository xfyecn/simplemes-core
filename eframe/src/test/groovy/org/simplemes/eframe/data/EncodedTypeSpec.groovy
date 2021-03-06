/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.AllFieldsDomain

/**
 * Tests.
 */
class EncodedTypeSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [AllFieldsDomain, FieldExtension]

  def "verify that Encoded Type can be saved and retrieved - as part of the two different domains"() {
    given: 'two domains with encoded types'
    def o2 = null
    FieldExtension.withTransaction {
      new FieldExtension(fieldName: 'ABC', fieldFormat: LongFieldFormat.instance, domainClassName: 'dummy').save()
      o2 = new AllFieldsDomain(name: 'ABC', status: DisabledStatus.instance, dueDate: new DateOnly()).save()
    }

    when: 'the records are re-read in another txn/session'
    def objectRead1 = null
    def objectRead2 = null
    FieldExtension.withTransaction {
      objectRead1 = FieldExtension.findAllByDomainClassName('dummy')[0]
      objectRead2 = AllFieldsDomain.findByUuid(o2.uuid)
    }

    then: 'the date only is correct'
    objectRead1.fieldFormat == LongFieldFormat.instance
    objectRead2.status == DisabledStatus.instance
  }


}
