/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain

import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.FieldSizes

import javax.persistence.Column

/**
 * Defines a Role for security.  These roles are used by the access points (controllers) to determine which users
 * can have access to the system.
 * <p>
 * The default roles include: 'ADMIN','CUSTOMIZER','MANAGER','DESIGNER'.
 */
@Slf4j
@MappedEntity()
@DomainEntity
@EqualsAndHashCode(includes = ['authority'])
class Role {

  /**
   * The Role name (authority).  Example: 'ADMIN'.
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String authority

  /**
   * The short title of this object.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH, nullable = false)
  String title

  /**
   * The internal unique ID for this record.
   */
  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid

  /**
   *  Build a human-readable version of this object.
   * @return The human-readable string.
   */
  @Override
  String toString() {
    // Use the authority to work around issue with User.list() not returning the title for the userRoles.
    return authority
  }

  /**
   * Defines the domain(s) this initial data must be loaded before/after.
   */
  @SuppressWarnings("unused")
  static initialDataLoadBefore = [User]

  /**
   * A list of the records created by the initial data load.
   * Used only for test cleanup by {@link org.simplemes.eframe.test.BaseSpecification}.
   */
  static initialDataRecords = [Role: ['ADMIN', 'CUSTOMIZER', 'MANAGER', 'DESIGNER']]


  /**
   * Load initial Role records.  Will make sure all of the needed roles exist.
   */
  static Map<String, List<String>> initialDataLoad() {
    def total = 0
    total += createRoleIfNeeded('ADMIN', 'Admin')
    total += createRoleIfNeeded('CUSTOMIZER', 'Customizer')
    total += createRoleIfNeeded('MANAGER', 'Manager')
    total += createRoleIfNeeded('DESIGNER', 'Designer')
    if (total) {
      log.debug("Created {} Role records", total)
    }
    return initialDataRecords
  }

  /**
   * Creates the role if it doesn't exist in the DB.
   * @param authority The role.
   * @param title The title.
   * return Number of records created.
   */
  static int createRoleIfNeeded(String authority, String title) {
    if (!findByAuthority(authority)) {
      new Role(authority: authority, title: title).save()
      return 1
    }
    return 0

  }


}
