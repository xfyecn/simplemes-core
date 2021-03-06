/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data

import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.LongFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.ISODate
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.system.DisabledStatus
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DataGenerator
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import org.simplemes.eframe.web.report.ReportTimeIntervalEnum
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/**
 * Tests.
 */
class CustomFieldDefinitionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [FieldGUIExtension, FieldExtension, AllFieldsDomain]

  @Rollback
  def "verify that FieldExtension constructor works"() {
    given: 'a custom field on a domain'
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: SampleParent.name,
                                            fieldFormat: BigDecimalFieldFormat.instance).save()

    when: 'the constructor is used'
    def field = new CustomFieldDefinition(fieldExtension)

    then: 'the correct fields properties are created'
    field.name == 'abc'
    field.type == BigDecimal
    field.format == BigDecimalFieldFormat.instance
    field.fieldExtensionUuid == fieldExtension.uuid
    field.label == "abc"
  }

  @Rollback
  def "verify that FieldExtension constructor works with a valueClassName"() {
    given: 'a custom field on a domain'
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: SampleParent.name,
                                            fieldFormat: EncodedTypeFieldFormat.instance,
                                            valueClassName: BasicStatus.name).save()

    when: 'the constructor is used'
    def field = new CustomFieldDefinition(fieldExtension)

    then: 'the correct fields properties are created'
    field.type == BasicStatus
  }

  @Rollback
  def "verify that FieldExtension constructor works with string length"() {
    given: 'a custom field on a domain'
    def fieldExtension = new FieldExtension(fieldName: 'xyz', domainClassName: SampleParent.name,
                                            maxLength: 237).save()

    when: 'the constructor is used'
    def field = new CustomFieldDefinition(fieldExtension)

    then: 'the correct fields properties are created'
    field.name == 'xyz'
    field.type == String
    field.format == StringFieldFormat.instance
    field.maxLength == 237
  }

  @Rollback
  def "verify that FieldExtension constructor works with unknown type"() {
    given: 'a custom field on a domain'
    def fieldExtension = new FieldExtension(fieldName: 'xyz', domainClassName: SampleParent.name,
                                            fieldFormat: DomainReferenceFieldFormat.instance).save()

    when: 'the constructor is used'
    def field = new CustomFieldDefinition(fieldExtension)

    then: 'the correct fields properties are created'
    field.name == 'xyz'
    field.type == Object
    field.format == DomainReferenceFieldFormat.instance
  }

  def "verify that Map constructor works"() {
    when: 'the constructor is used'
    def field = new CustomFieldDefinition(name: 'title', type: Integer)

    then: 'the correct field is created'
    field.name == 'title'
    field.type == Integer
  }

  @Rollback
  def "verify that get and setFieldValue works with a domain with ExtensibleFields annotation"() {
    given: 'a custom field on a domain'
    def fieldExtension = new FieldExtension(fieldName: 'xyz', domainClassName: SampleParent.name,
                                            maxLength: 237).save()
    def fieldDef = new CustomFieldDefinition(fieldExtension)

    and: 'a domain object'
    def sampleParent = new SampleParent()

    when: 'the value is set'
    fieldDef.setFieldValue(sampleParent, 'ABC')

    then: 'the values can be read'
    fieldDef.getFieldValue(sampleParent) == 'ABC'

    and: 'the value is stored in the domains custom fields holder'
    sampleParent.customFields.contains('xyz')
    sampleParent.customFields.contains('ABC')
  }

  static aDate = new Date(UnitTestUtils.SAMPLE_TIME_MS)
  static aDateOnly = new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)

  def "verify that get and setFieldValue works with custom fields on supported types"() {
    given: 'a custom field on a domain'
    def vcn = vc?.name
    def fieldExtension = DataGenerator.buildCustomField(domainClass: SampleParent, fieldName: 'xyz', fieldFormat: format.instance,
                                                        valueClassName: vcn)
    def fieldDef = new CustomFieldDefinition(fieldExtension)

    and: 'a domain object'
    def sampleParent = new SampleParent()

    when: 'the value is set'
    if (value == AllFieldsDomain) {
      // Special case to allow creation of record for domain reference where value
      AllFieldsDomain.withTransaction {
        value = new AllFieldsDomain(name: 'XYZ').save()
      }
      contains = value.uuid.toString()
    }
    fieldDef.setFieldValue(sampleParent, value)

    then: 'the values can be read'
    fieldDef.getFieldValue(sampleParent) == value

    and: 'the value is stored in the domains custom fields holder'
    sampleParent.customFields.contains(contains)

    where:
    format                     | value                            | vc                     | contains
    StringFieldFormat          | 'ABC'                            | null                   | '"ABC"'
    BigDecimalFieldFormat      | 1.2                              | null                   | '1.2'
    IntegerFieldFormat         | 237                              | null                   | '237'
    LongFieldFormat            | 4237                             | null                   | '4237'
    BooleanFieldFormat         | true                             | null                   | 'true'
    DateFieldFormat            | aDate                            | null                   | '2009-02-13T23:31:30.456Z'
    DateOnlyFieldFormat        | aDateOnly                        | null                   | ISODate.format(aDateOnly)
    EnumFieldFormat            | ReportTimeIntervalEnum.YESTERDAY | ReportTimeIntervalEnum | ReportTimeIntervalEnum.YESTERDAY.toString()
    EncodedTypeFieldFormat     | DisabledStatus.instance          | BasicStatus            | DisabledStatus.instance.id
    DomainReferenceFieldFormat | AllFieldsDomain                  | AllFieldsDomain        | AllFieldsDomain.name
    //ChildListFieldFormat       | GridWidget
    //DomainRefListFieldFormat   | MultiComboboxWidget
  }

  @Rollback
  def "verify that FieldExtension constructor uses a specified label if defined in the field extension"() {
    given: 'a custom field on a domain'
    def fieldExtension = new FieldExtension(fieldName: 'abc', domainClassName: SampleParent.name,
                                            fieldLabel: 'Custom Label',
                                            fieldFormat: BigDecimalFieldFormat.instance).save()

    when: 'the constructor is used'
    def field = new CustomFieldDefinition(fieldExtension)

    then: 'the label is used'
    field.label == "Custom Label"
  }


}
