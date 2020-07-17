/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.assy.demand.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository
import org.simplemes.mes.demand.domain.Order

/**
 * The repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface OrderAssembledComponentRepository extends BaseRepository, CrudRepository<OrderAssembledComponent, UUID> {

  Optional<OrderAssembledComponent> findByUuid(UUID uuid)

  List<OrderAssembledComponent> findAllByOrder(Order order)

  List<OrderAssembledComponent> list()

}
