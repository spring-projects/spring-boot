/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.neo4j.domain.country;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;

public interface ReactiveCountryRepository extends ReactiveNeo4jRepository<Country, Long> {

}
