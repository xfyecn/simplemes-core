/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect

/**
 * The H2-Specific version of the repo.
 */
@JdbcRepository(dialect = Dialect.H2)
@Requires(env = [Environment.TEST])
@Replaces(CustomOrderComponentRepository)
interface CustomOrderComponentRepositoryH2 extends CustomOrderComponentRepository {
}