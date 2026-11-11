/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.domain.city;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface ReactiveCityRepository extends ReactiveNeo4jRepository<City, Long> {

}
