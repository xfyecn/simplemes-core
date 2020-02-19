/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.mes.floor.domain


import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.CrudRepository
import org.simplemes.eframe.domain.BaseRepository

/**
 * The sample WorkCenter repository base interface.  Provides the methods for the repo,
 * but sub-classes need to implement the dialect needed.  The sub-classes will be the concrete
 * beans generated for the runtime.
 */
interface WorkCenterRepository extends BaseRepository, CrudRepository<WorkCenter, UUID> {

  Optional<WorkCenter> findByWorkCenter(String workCenter)

  Optional<WorkCenter> findByUuid(UUID uuid)

  List<WorkCenter> list(Pageable pageable)

  List<WorkCenter> list()

}
