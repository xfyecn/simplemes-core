/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.product.domain

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The Product repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
interface ProductRepository extends BaseRepository, CrudRepository<Product, UUID> {

  Optional<Product> findByUuid(UUID uuid)

  Optional<Product> findByProduct(String product)

  List<Product> list(Pageable pageable)

  List<Product> list()

}
